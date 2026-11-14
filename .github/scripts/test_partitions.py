import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('gate', Path(__file__).with_name('verify-partitions.py'))
gate = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gate)

class PartitionGateTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        expected = ['a.ATest > one', 'b.BTest > two[30]', 'b.BTest > two']
        for i, classes in enumerate([['a/ATest.class'], ['b/BTest.class']]):
            p = self.root/f'test-partition-{i}'
            m = p/'test-partition'; m.mkdir(parents=True)
            (m/'all-classes.txt').write_text('a/ATest.class\nb/BTest.class\n')
            (m/'selected-classes.txt').write_text('\n'.join(classes)+'\n')
            (m/'expected-tests.json').write_text(json.dumps(expected))
            r = p/'test-results/testDebugUnitTest'; r.mkdir(parents=True)
            suite = ET.Element('testsuite')
            for key in expected[:1] if i == 0 else expected[1:]:
                cls, method = key.split(' > ')
                ET.SubElement(suite, 'testcase', classname=cls, name=method)
            ET.ElementTree(suite).write(r/'TEST-results.xml')
    def tearDown(self): self.temp.cleanup()
    def verify(self): gate.verify(self.root)
    def path(self, i, suffix): return self.root/f'test-partition-{i}'/suffix
    def xml(self, i): return self.path(i, 'test-results/testDebugUnitTest/TEST-results.xml')
    def test_exact_union_passes(self): self.verify()
    def test_failure_error_and_skip_each_refused(self):
        p = self.xml(1); original = p.read_bytes()
        for tag in ['failure', 'error', 'skipped']:
            root = ET.fromstring(original); ET.SubElement(root.find('testcase'), tag); ET.ElementTree(root).write(p)
            with self.assertRaises(ValueError): self.verify()
        p.write_bytes(original)
    def test_omitted_method_refused(self):
        p = self.xml(1); tree = ET.parse(p); root=tree.getroot();root.remove(root.find('testcase'));tree.write(p)
        with self.assertRaises(ValueError): self.verify()
    def test_duplicate_method_refused(self):
        p=self.xml(1);tree=ET.parse(p);root=tree.getroot();ET.SubElement(root,'testcase',classname='b.BTest',name='two');tree.write(p)
        with self.assertRaises(ValueError): self.verify()
    def test_wrong_partition_refused(self):
        p=self.xml(1);tree=ET.parse(p);tree.getroot().find('testcase').set('classname','a.ATest');tree.write(p)
        with self.assertRaises(ValueError): self.verify()
    def test_overlap_refused(self):
        self.path(1,'test-partition/selected-classes.txt').write_text('a/ATest.class\nb/BTest.class\n')
        with self.assertRaises(ValueError): self.verify()
    def test_missing_class_refused(self):
        self.path(1,'test-partition/selected-classes.txt').write_text('')
        with self.assertRaises(ValueError): self.verify()
    def test_manifest_difference_refused(self):
        self.path(1,'test-partition/expected-tests.json').write_text('[]')
        with self.assertRaises(ValueError): self.verify()
    def test_missing_results_refused(self):
        self.xml(1).unlink()
        with self.assertRaises(ValueError): self.verify()

if __name__ == '__main__': unittest.main()
