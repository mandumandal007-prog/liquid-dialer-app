package com.mandu.liquiddialer
import com.mandu.liquiddialer.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class) @Config(sdk=[34])
class EstimateBundlesTest {
 private fun base()=CallEstimate.Draft(17,listOf(CallEstimate.line("Rice","2.5","40","a"),CallEstimate.line("Oil","1","120","b")),"10","5")
 private fun rows()=EstimateBundles.capture(emptyList(),"Weekly",base(),setOf("b","a"))
 private fun raw()=EstimateBundles.encode(rows())
 private fun fail(f:()->Unit){assertThrows(Exception::class.java,f)}
 @Test fun defaultOffEmpty(){assertFalse(LdSettings().estimateBundles);assertEquals(emptyList<EstimateBundles.Bundle>(),EstimateBundles.decode(LdSettings().estimateBundleLibrary))}
 @Test fun captureSelectedOrderOnlyAndFreshIds(){val r=EstimateBundles.capture(emptyList()," Choice ",base(),setOf("b"))[0];assertEquals("Choice",r.name);assertEquals("Oil",r.lines.single().label);assertNotEquals("b",r.lines.single().id);assertEquals("1",r.lines.single().quantity);assertEquals("120",r.lines.single().rate)}
 @Test fun roundtripUnicode(){val r=EstimateBundles.capture(emptyList(),"रोज वाला",base(),setOf("a","b"));assertEquals(r,EstimateBundles.decode(EstimateBundles.encode(r)));assertEquals(listOf("Rice","Oil"),r[0].lines.map{it.label})}
 @Test fun sixLimitNoTruncate(){var r=rows();repeat(5){r=EstimateBundles.capture(r,"B$it",base(),setOf("a"))};fail{EstimateBundles.capture(r,"Seventh",base(),setOf("a"))};assertEquals(6,EstimateBundles.decode(EstimateBundles.encode(r)).size)}
 @Test fun namesStrict(){for(n in listOf(""," ","a".repeat(33),"A\nB","A\tB"))fail{EstimateBundles.capture(emptyList(),n,base(),setOf("a"))};fail{EstimateBundles.capture(rows(),"weekly",base(),setOf("a"))}}
 @Test fun selectionMustBeExistingNonempty(){fail{EstimateBundles.capture(emptyList(),"x",base(),emptySet())};fail{EstimateBundles.capture(emptyList(),"x",base(),setOf("missing"))}}
 @Test fun badDraftAndControlNamesRefused(){fail{EstimateBundles.capture(emptyList(),"x",base().copy(discount="9999"),setOf("a"))};val d=CallEstimate.Draft(lines=listOf(CallEstimate.line("A\nB","1","2","x")));fail{EstimateBundles.capture(emptyList(),"x",d,setOf("x"))}}
 @Test fun renameAndDeletePreserveOtherRows(){val r=EstimateBundles.capture(rows(),"Other",base(),setOf("a"));val n=EstimateBundles.rename(r,r[0].id,"Changed");assertEquals(r[0].copy(name="Changed"),n[0]);assertEquals(r[1],n[1]);assertEquals(listOf(r[1]),EstimateBundles.remove(n,n[0].id))}
 @Test fun staleMissingIdsRefused(){fail{EstimateBundles.rename(rows(),"missing","x")};fail{EstimateBundles.remove(rows(),"missing")};val r=EstimateBundles.capture(rows(),"Other",base(),setOf("a"));fail{EstimateBundles.rename(r,r[0].id,"other")}}
 @Test fun malformedLibraryLocked(){for(s in listOf("null","[]","{}","broken",raw()+"x","x".repeat(80001)))fail{EstimateBundles.decode(s)}}
 @Test fun versionAndTypesStrict(){for(v in listOf(2,"1",true)){val o=org.json.JSONObject(raw()).put("version",v);fail{EstimateBundles.decode(o.toString())}};val o=org.json.JSONObject(raw());o.getJSONArray("bundles").getJSONObject(0).getJSONArray("lines").getJSONObject(0).put("rate",40);fail{EstimateBundles.decode(o.toString())}}
 @Test fun duplicateIdentityNameAndRowsRefused(){val r=rows()[0];fail{EstimateBundles.encode(listOf(r,r))};fail{EstimateBundles.encode(listOf(r,r.copy(id="Other")))};fail{EstimateBundles.encode(listOf(r.copy(lines=listOf(r.lines[0],r.lines[0]))))}}
 @Test fun emptyAndOverfullBundleRefused(){val r=rows()[0];fail{EstimateBundles.encode(listOf(r.copy(lines=emptyList())))};fail{EstimateBundles.encode(listOf(r.copy(lines=(0..20).map{r.lines[0].copy(id="x$it")})))}}
 @Test fun appendPreservesWholeSourceNoMerge(){val r=rows();val lib=EstimateBundles.encode(r);val b=base();val p=EstimateBundles.plan(b,lib,r[0].id);assertEquals(b.lines,p.after.lines.take(2));assertEquals(listOf("Rice","Oil","Rice","Oil"),p.after.lines.map{it.label});assertEquals(b.discount,p.after.discount);assertEquals(b.tax,p.after.tax);assertEquals(b.revision,p.after.revision);assertEquals(4,p.after.lines.map{it.id}.distinct().size);assertEquals(p.after,EstimateBundles.apply(p,b,lib));assertEquals("451.50",CallEstimate.money(CallEstimate.totals(p.after).total))}
 @Test fun emptyCalculatorWorks(){val r=rows();val p=EstimateBundles.plan(CallEstimate.Draft(),EstimateBundles.encode(r),r[0].id);assertEquals(2,p.after.lines.size);assertEquals("0",p.after.discount);assertEquals("0",p.after.tax)}
 @Test fun capacityAllOrNothing(){val r=rows();val d=base().copy(lines=(0..18).map{CallEstimate.line("Existing","1","1","e$it")},discount="0");fail{EstimateBundles.plan(d,EstimateBundles.encode(r),r[0].id)};val p=EstimateBundles.plan(d.copy(lines=d.lines.take(18)),EstimateBundles.encode(r),r[0].id);assertEquals(20,p.after.lines.size)}
 @Test fun staleDraftOrLibraryRefused(){val r=rows();val lib=EstimateBundles.encode(r);val p=EstimateBundles.plan(base(),lib,r[0].id);fail{EstimateBundles.apply(p,base().copy(tax="6"),lib)};fail{EstimateBundles.apply(p,base(),EstimateBundles.encode(EstimateBundles.rename(r,r[0].id,"Changed")))}}
 @Test fun tamperedPreviewRefused(){val r=rows();val lib=EstimateBundles.encode(r);val p=EstimateBundles.plan(base(),lib,r[0].id);fail{EstimateBundles.apply(p.copy(after=p.after.copy(tax="9")),base(),lib)};fail{EstimateBundles.apply(p.copy(token="bad"),base(),lib)}}
 @Test fun sourceIdsNeverLeakIntoAppendedRows(){val r=rows();val p=EstimateBundles.plan(base(),EstimateBundles.encode(r),r[0].id);assertTrue(p.after.lines.drop(2).none{x->r[0].lines.any{it.id==x.id}})}
 @Test fun unchangedPricesAmountsAndRounding(){val b=CallEstimate.Draft(lines=listOf(CallEstimate.line("Fraction","0.005","1.25","a")));val r=EstimateBundles.capture(emptyList(),"Small",b,setOf("a"));val p=EstimateBundles.plan(CallEstimate.Draft(),EstimateBundles.encode(r),r[0].id);assertEquals("0.01",CallEstimate.money(CallEstimate.totals(p.after).subtotal));assertEquals("0.005",p.after.lines[0].quantity);assertEquals("1.25",p.after.lines[0].rate)}
 @Test fun finderRoutes(){assertTrue(SettingsFinder.search("reusable bundle quantity rate").any{it.route=="estimatebundles"})}
}
