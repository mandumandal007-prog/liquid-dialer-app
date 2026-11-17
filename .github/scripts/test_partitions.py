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
        expected = ['a.ATest > one', 'b.BTest > two[30]', 'b.BTest > two', 'c.CTest > three']
        for i, classes in enumerate([['a/ATest.class'], ['b/BTest.class'], ['c/CTest.class']]):
            p = self.root/f'test-partition-{i}'
            m = p/'test-partition'; m.mkdir(parents=True)
            (m/'all-classes.txt').write_text('a/ATest.class\nb/BTest.class\nc/CTest.class\n')
            (m/'selected-classes.txt').write_text('\n'.join(classes)+'\n')
            (m/'expected-tests.json').write_text(json.dumps(expected))
            r = p/'test-results/testDebugUnitTest'; r.mkdir(parents=True)
            suite = ET.Element('testsuite')
            for key in [expected[0]] if i == 0 else expected[1:3] if i == 1 else expected[3:]:
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
        self.path(1,'test-partition/selected-classes.txt').write_text('a/ATest.class\nb/BTest.class\nc/CTest.class\n')
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

    def test_third_partition_failure_skip_error_refused(self):
        p = self.xml(2); original = p.read_bytes()
        for tag in ['failure', 'error', 'skipped']:
            root = ET.fromstring(original); ET.SubElement(root.find('testcase'), tag); ET.ElementTree(root).write(p)
            with self.assertRaises(ValueError): self.verify()
        p.write_bytes(original)
    def test_third_partition_overlap_refused(self):
        self.path(2, 'test-partition/selected-classes.txt').write_text('b/BTest.class\nc/CTest.class\n')
        with self.assertRaises(ValueError): self.verify()
    def test_third_manifest_difference_refused(self):
        self.path(2, 'test-partition/expected-tests.json').write_text('[]')
        with self.assertRaises(ValueError): self.verify()
    def test_third_missing_result_refused(self):
        self.xml(2).unlink()
        with self.assertRaises(ValueError): self.verify()
    def test_third_omitted_method_refused(self):
        p=self.xml(2); tree=ET.parse(p); tree.getroot().remove(tree.getroot().find('testcase')); tree.write(p)
        with self.assertRaises(ValueError): self.verify()
    def test_third_duplicate_method_refused(self):
        p=self.xml(2); tree=ET.parse(p); ET.SubElement(tree.getroot(),'testcase',classname='c.CTest',name='three'); tree.write(p)
        with self.assertRaises(ValueError): self.verify()
    def test_third_wrong_partition_refused(self):
        p=self.xml(2); tree=ET.parse(p); tree.getroot().find('testcase').set('classname','a.ATest'); tree.write(p)
        with self.assertRaises(ValueError): self.verify()
    def test_third_missing_class_refused(self):
        self.path(2,'test-partition/selected-classes.txt').write_text('')
        with self.assertRaises(ValueError): self.verify()
    def test_third_all_classes_difference_refused(self):
        self.path(2,'test-partition/all-classes.txt').write_text('different.class\n')
        with self.assertRaises(ValueError): self.verify()
    def test_missing_third_directory_refused(self):
        import shutil
        shutil.rmtree(self.root/'test-partition-2')
        with self.assertRaises(ValueError): self.verify()
    def test_unexpected_fourth_directory_refused(self):
        (self.root/'test-partition-3').mkdir()
        with self.assertRaises(ValueError): self.verify()

if __name__ == '__main__': unittest.main()
