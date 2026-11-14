#!/usr/bin/env python3
"""Fail closed unless both partition result sets cover the exact compiled @Test manifest."""
import argparse
import collections
import json
import struct
from pathlib import Path
import xml.etree.ElementTree as ET


def annotated_tests(data):
    pos = 0
    def read(n):
        nonlocal pos
        value = data[pos:pos+n]
        if len(value) != n: raise ValueError('Truncated class')
        pos += n
        return value
    def u1(): return read(1)[0]
    def u2(): return struct.unpack('>H', read(2))[0]
    def u4(): return struct.unpack('>I', read(4))[0]
    if u4() != 0xCAFEBABE: raise ValueError('Invalid class magic')
    read(4)
    cp = [None] * u2()
    i = 1
    while i < len(cp):
        tag = u1()
        if tag == 1: cp[i] = read(u2()).replace(b'\xc0\x80', b'\x00').decode('utf-8', errors='surrogatepass')
        elif tag == 3: cp[i] = struct.unpack('>i', read(4))[0]
        elif tag == 4: read(4)
        elif tag in (5, 6): read(8); i += 1
        elif tag in (7, 8, 16, 19, 20): cp[i] = u2()
        elif tag in (9, 10, 11, 12, 17, 18): read(4)
        elif tag == 15: read(3)
        else: raise ValueError(f'Unknown constant tag {tag}')
        i += 1
    read(2)
    name = cp[cp[u2()]].replace('/', '.')
    read(2)
    read(u2()*2)
    def attributes():
        return [(cp[u2()], read(u4())) for _ in range(u2())]
    def annotations(attrs):
        found = {}
        for key, raw in attrs:
            if key != 'RuntimeVisibleAnnotations': continue
            at = 0
            def a2():
                nonlocal at
                v = struct.unpack('>H', raw[at:at+2])[0]; at += 2; return v
            def value():
                nonlocal at
                tag = chr(raw[at]); at += 1
                if tag in 'BCDFIJSZsc': return cp[a2()]
                if tag == 'e': return (cp[a2()], cp[a2()])
                if tag == '@': return annotation()
                if tag == '[': return [value() for _ in range(a2())]
                raise ValueError(f'Unknown annotation value {tag}')
            def annotation():
                typ = cp[a2()]
                values = {}
                for _ in range(a2()):
                    key = cp[a2()]; values[key] = value()
                return typ, values
            for _ in range(a2()):
                typ, values = annotation(); found[typ] = values
            if at != len(raw): raise ValueError('Trailing annotation bytes')
        return found
    for _ in range(u2()): read(6); attributes()
    methods = []
    for _ in range(u2()):
        read(2); method = cp[u2()]; read(2)
        anns = annotations(attributes())
        if 'Lorg/junit/Test;' in anns: methods.append((method, anns))
    class_anns = annotations(attributes())
    config = 'Lorg/robolectric/annotation/Config;'
    expanded = []
    for method, anns in methods:
        sdks = anns.get(config, {}).get('sdk', class_anns.get(config, {}).get('sdk', []))
        if any(s <= 0 for s in sdks): raise ValueError('Symbolic SDK constants require explicit manifest support')
        # Robolectric runs the highest configured SDK without a suffix, and the rest with [sdk].
        expanded.extend(f'{method}[{sdk}]' for sdk in sorted(set(sdks))[:-1])
        expanded.append(method)
    return name, expanded


def compile_manifest(roots):
    tests = []
    for root in roots:
        for path in sorted(Path(root).rglob('*.class')):
            name, methods = annotated_tests(path.read_bytes())
            tests.extend(f'{name} > {method}' for method in methods)
    if not tests or len(tests) != len(set(tests)):
        raise ValueError('Empty or duplicate compiled test manifest')
    return sorted(tests)


def verify(artifacts):
    parts = [Path(artifacts) / f'test-partition-{i}' for i in range(2)]
    all_sets = [set((p/'test-partition/all-classes.txt').read_text().splitlines()) for p in parts]
    assigned = [set((p/'test-partition/selected-classes.txt').read_text().splitlines()) for p in parts]
    expected = [set(json.loads((p/'test-partition/expected-tests.json').read_text())) for p in parts]
    if not all_sets[0] or all_sets[0] != all_sets[1] or expected[0] != expected[1]:
        raise ValueError('Partition compiled manifests differ or are empty')
    if assigned[0] & assigned[1] or assigned[0] | assigned[1] != all_sets[0]:
        raise ValueError('Compiled class partitions overlap or omit classes')
    found = []
    for i, part in enumerate(parts):
        xmls = list((part/'test-results/testDebugUnitTest').glob('*.xml'))
        if not xmls: raise ValueError(f'No test XMLs in partition {i}')
        current = []
        for xml in xmls:
            root = ET.parse(xml).getroot()
            if root.findall('.//failure') or root.findall('.//error') or root.findall('.//skipped'):
                raise ValueError(f'Nonpassing test result {xml.name}')
            for case in root.findall('.//testcase'):
                key = f"{case.attrib['classname']} > {case.attrib['name']}"
                cls = case.attrib['classname'].replace('.', '/') + '.class'
                if cls not in assigned[i]: raise ValueError(f'Test in wrong partition: {key}')
                current.append(key)
        if len(current) != len(set(current)): raise ValueError('Duplicate test results')
        found.extend(current)
        print(f'Partition {i}: {len(current)} passing tests')
    if len(found) != len(set(found)) or set(found) != expected[0]:
        raise ValueError(f'Test coverage mismatch: missing={sorted(expected[0]-set(found))}, extra={sorted(set(found)-expected[0])}')
    print(f'VERIFIED: {len(found)} distinct passing tests exactly cover full compiled @Test manifest; no skip/failure/error')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--compiled-roots', nargs='+')
    parser.add_argument('--output')
    parser.add_argument('--artifacts')
    args = parser.parse_args()
    if args.compiled_roots:
        tests = compile_manifest(args.compiled_roots)
        Path(args.output).write_text(json.dumps(tests, indent=2)+'\n')
        print(f'Compiled @Test manifest: {len(tests)} methods')
    elif args.artifacts: verify(args.artifacts)
    else: parser.error('Choose manifest generation or artifact verification')
