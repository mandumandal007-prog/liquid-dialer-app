#!/usr/bin/env python3
"""One-release recovery audit. Original full-run failures remain failures."""
import hashlib, importlib.util, json, os, re, subprocess, sys, urllib.request, urllib.error, urllib.parse, zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
SHA='46350034127ae1a95ab243dd1c0b9d6fbb6acd77'
RUN=44762529869
REPO='mandumandal007-prog/liquid-dialer-app'
CLASS='com.mandu.liquiddialer.CallReferencesPrivacyUiTest'
FAILED=CLASS+' > settingsEntryAndCallToggleGate'
DIGEST='64b864afc1be9ba66690cad703bcb9e9e87f30157ca3ec04baff5b9c60aa3847'
ARTIFACTS=[(14646189421,'0d18f9429f5717714f770e2e4fd9ff2618f3a8a12e639141f12c460484f04535'),(14646159528,'be43a92a4db63ecb42f4afb8515ff133961f17d7733d8498d36644ba2d2f9681'),(14646502709,'4ffd313a2cbee9204ed9b12dffe72ce4b0fe0c1c0a2ebb2327e62aedf3c41ca9')]
JOBS=[136378500671,136378500838,136378500459]
METHODS={'corruptPreservedAndNoAdd','dayHiddenAndRevealedPixels','deletionRequiresConfirmation','settingsEntryAndCallToggleGate','staleDraftCannotOverwriteNewerCard'}
OUT=Path('recovery-evidence');OUT.mkdir(exist_ok=True)
class NoRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,req,fp,code,msg,headers,newurl):return None
def download_redirect(req):
 try:return urllib.request.build_opener(NoRedirect).open(req,timeout=90).read()
 except urllib.error.HTTPError as e:
  if e.code not in (301,302,303,307,308):raise
  target=urllib.parse.urljoin(req.full_url,e.headers['Location'])
  u=urllib.parse.urlsplit(target)
  if u.scheme!='https' or not u.hostname:raise ValueError('Invalid download redirect')
  # The signed storage URL authenticates itself. Never forward the API token.
  clean=urllib.request.Request(target,headers={'Accept':'application/octet-stream'})
  return urllib.request.urlopen(clean,timeout=90).read()
def api(path):
 req=urllib.request.Request('https://api.github.com/repos/'+REPO+path,headers={'Authorization':'Bearer '+os.environ['GH_TOKEN'],'Accept':'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28','Cache-Control':'no-cache'})
 return download_redirect(req)
def require(ok,msg):
 if not ok:raise SystemExit(msg)
def events(text):
 rows=re.findall(r'^\S+Z (.+? > .+?) (STARTED|PASSED|FAILED|SKIPPED)\s*$',text,re.M)
 return {k:[('com.mandu.liquiddialer.'+n if not n.startswith('com.') else n) for n,t in rows if t==k]for k in ['STARTED','PASSED','FAILED','SKIPPED']}
def compiled():
 spec=importlib.util.spec_from_file_location('original_verifier','.github/scripts/verify-partitions.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
 return set(m.compile_manifest(['app/build/tmp/kotlin-classes/debugUnitTest','app/build/intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes']))
require(subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()==SHA,'Wrong source commit')
require(not subprocess.check_output(['git','diff','--name-only'],text=True).strip(),'Tracked source changed')
mode=sys.argv[1]
if mode=='full':
 run=json.loads(api('/actions/runs/'+str(RUN)));require(run['head_sha']==SHA and run['status']=='completed' and run['conclusion']=='failure' and run['run_attempt']==1,'Unexpected original run state')
 (OUT/'original-run.json').write_text(json.dumps(run,indent=2))
 starts=[];passes=[];failures=[];xmlnames=[];expected=None;classsets=[];assigned=[]
 for i,(aid,digest)in enumerate(ARTIFACTS):
  data=api('/actions/artifacts/'+str(aid)+'/zip');require(hashlib.sha256(data).hexdigest()==digest,'Original artifact digest changed')
  zpath=OUT/f'original-partition-{i}.zip';zpath.write_bytes(data);root=OUT/f'original-partition-{i}'
  with zipfile.ZipFile(zpath)as z:z.extractall(root)
  manifest=(root/'test-partition/expected-tests.json').read_bytes();require(hashlib.sha256(manifest).hexdigest()==DIGEST,'Manifest digest changed')
  current=set(json.loads(manifest));expected=current if expected is None else expected;require(current==expected,'Partition manifest mismatch')
  classsets.append(set((root/'test-partition/all-classes.txt').read_text().splitlines()));assigned.append(set((root/'test-partition/selected-classes.txt').read_text().splitlines()))
  raw=api('/actions/jobs/'+str(JOBS[i])+'/logs').decode();(OUT/f'original-raw-{i}.txt').write_text(raw);ev=events(raw)
  for k,v in ev.items():require(len(v)==len(set(v)),'Duplicate raw '+k)
  require(not ev['SKIPPED'],'Original skipped test');starts+=ev['STARTED'];passes+=ev['PASSED'];failures+=ev['FAILED']
  cases=[];xf=[]
  for p in (root/'test-results/testDebugUnitTest').glob('*.xml'):
   r=ET.parse(p).getroot();require(not r.findall('.//error') and not r.findall('.//skipped'),'Original error/skip')
   for c in r.findall('testcase'):
    n=c.attrib['classname']+' > '+c.attrib['name'];cases.append(n);require(c.attrib['classname'].replace('.','/')+'.class' in assigned[-1],'Wrong assigned class')
    if c.find('failure')is not None:xf.append(n)
  require(len(cases)==len(set(cases))and set(cases)==set(ev['STARTED'])and set(xf)==set(ev['FAILED']),'Raw/XML mismatch');xmlnames+=cases
 require(all(x==classsets[0]for x in classsets),'All-class manifest mismatch')
 require(not any(assigned[i]&assigned[j]for i in range(3)for j in range(i))and set.union(*assigned)==classsets[0],'Class partition coverage mismatch')
 require(len(starts)==len(set(starts))==3143 and set(starts)==expected==compiled(),'Full compiled coverage mismatch')
 require(len(passes)==len(set(passes))==3142 and set(passes)==expected-{FAILED}and failures==[FAILED],'Original failure set changed')
 require({n.split(' > ')[1]for n in expected if n.startswith(CLASS+' > ')}==METHODS,'Class method manifest changed')
 (OUT/'full-audit.json').write_text(json.dumps({'source_sha':SHA,'full_run':RUN,'manifest_methods':3143,'original_passed':3142,'original_failed':[FAILED],'recovery_not_yet_cleared':True},indent=2));print('FULL EVIDENCE AUDIT PASSED, ORIGINAL RUN STILL FAILED')
elif mode=='confirmed':
 run=json.loads(api('/actions/runs/44776413534'));require(run['head_sha']=='73e526417ab76cedea17059547dd3ee561b52f5e' and run['run_attempt']==1 and run['conclusion']=='failure','Wrong confirmation provenance')
 jobs=json.loads(api('/actions/runs/44776413534/jobs'))['jobs'];require(len(jobs)==1,'Unexpected confirmation jobs');states={x['name']:x['conclusion']for x in jobs[0]['steps']}
 require(states['Audit original full run without replacing its failure']=='success'and states['One isolated five-test class confirmation, no retries']=='success'and states['Build frozen release APK']=='success'and states['Verify actual APK identity, signature and hash']=='failure','Confirmation did not pass before parser stop')
 data=api('/actions/artifacts/14650821073/zip');require(hashlib.sha256(data).hexdigest()=='b99fe0eadd34aa67e26f7676f2e029adc1b32b6c6f132a23da7598bd3a1002ec','Pinned confirmation artifact changed')
 zpath=OUT/'pinned-confirmation-evidence.zip';zpath.write_bytes(data);root=OUT/'pinned-confirmation'
 with zipfile.ZipFile(zpath)as z:z.extractall(root)
 audit=json.loads((root/'recovery-evidence/isolated-audit.json').read_text());require(audit['source_sha']==SHA and audit['full_run']==RUN and audit['full_run_passed']is False and audit['original_failure']==FAILED and audit['test_attempts']==1,'Confirmation audit mismatch')
 require((root/'recovery-evidence/policy-sha.txt').read_text().strip()=='73e526417ab76cedea17059547dd3ee561b52f5e','Confirmation policy mismatch')
 text=(root/'isolated.log').read_text();rows=re.findall(r'^(\S+ > \S+) (STARTED|PASSED|FAILED|SKIPPED)\s*$',text,re.M)
 ev={k:['com.mandu.liquiddialer.'+n for n,t in rows if t==k]for k in ['STARTED','PASSED','FAILED','SKIPPED']};expected={CLASS+' > '+n for n in METHODS}
 require(len(ev['STARTED'])==len(ev['PASSED'])==5 and set(ev['STARTED'])==set(ev['PASSED'])==expected and not ev['FAILED']and not ev['SKIPPED'],'Pinned isolated raw gate failed')
 cases=[]
 for p in (root/'app/build/test-results/testDebugUnitTest').glob('*.xml'):
  r=ET.parse(p).getroot();require(not r.findall('.//failure')and not r.findall('.//error')and not r.findall('.//skipped'),'Pinned XML nonpass');cases +=[c.attrib['classname']+' > '+c.attrib['name']for c in r.findall('testcase')]
 require(len(cases)==5 and set(cases)==expected==set(audit['isolated_passed']),'Pinned XML coverage mismatch')
 (OUT/'reused-confirmation-audit.json').write_text(json.dumps({'source_sha':SHA,'confirmation_run':44776413534,'confirmation_artifact':14650821073,'artifact_sha256':hashlib.sha256(data).hexdigest(),'isolated_passed':sorted(expected),'tests_executed_in_this_build_job':0,'build_is_separate_from_confirmation':True,'original_full_run_passed':False},indent=2));print('PINNED PRIOR FIVE-TEST CONFIRMATION VERIFIED, NO TESTS RERUN')
elif mode=='isolated':
 require(len(compiled())==3143,'Compiled manifest changed')
 text=Path('isolated.log').read_text();ev=events(text)
 # Gradle local stdout is not timestamp-prefixed. Parse its original event lines.
 rows=re.findall(r'^(\S+ > \S+) (STARTED|PASSED|FAILED|SKIPPED)\s*$',text,re.M)
 ev={k:['com.mandu.liquiddialer.'+n for n,t in rows if t==k]for k in ['STARTED','PASSED','FAILED','SKIPPED']}
 expected={CLASS+' > '+n for n in METHODS};require(len(ev['STARTED'])==len(ev['PASSED'])==5 and set(ev['STARTED'])==set(ev['PASSED'])==expected and not ev['FAILED']and not ev['SKIPPED'],'Isolated raw gate failed')
 allcases=[]
 for p in Path('app/build/test-results/testDebugUnitTest').glob('*.xml'):
  r=ET.parse(p).getroot();require(not r.findall('.//failure')and not r.findall('.//error')and not r.findall('.//skipped'),'Isolated XML nonpass')
  allcases += [c.attrib['classname']+' > '+c.attrib['name']for c in r.findall('testcase')]
 require(len(allcases)==5 and set(allcases)==expected,'Isolated XML coverage mismatch')
 (OUT/'isolated-audit.json').write_text(json.dumps({'source_sha':SHA,'full_run':RUN,'full_run_passed':False,'original_failure':FAILED,'isolated_class':CLASS,'isolated_passed':sorted(expected),'test_attempts':1,'acceptance':'explicit one-release exception, not an all-green full run'},indent=2));print('ONE-RELEASE RECOVERY GATE PASSED')
else:raise SystemExit('Unknown mode')
