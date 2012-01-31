/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.guvnor.server.jaxrs;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.*;
import org.apache.abdera.protocol.Response.ResponseType;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.drools.guvnor.client.common.AssetFormats;
import org.drools.guvnor.server.ServiceImplementation;
import org.drools.guvnor.server.jaxrs.jaxb.Package;
import org.drools.guvnor.server.jaxrs.jaxb.PackageMetadata;
import org.drools.guvnor.server.util.DroolsHeader;
import org.drools.repository.AssetItem;
import org.drools.repository.PackageItem;
import org.drools.util.codec.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mvel2.util.StringAppender;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class BasicPackageResourceTest extends AbstractBusClientServerTestBase {
    private Abdera abdera = new Abdera();
    private static RestTestingBase restTestingBase;

    @BeforeClass
    public static void startServers() throws Exception {
       	restTestingBase = new RestTestingBase();
       	restTestingBase.setup();       	

        assertTrue("server did not launch correctly",
                   launchServer(CXFJAXRSServer.class, true));

        
        ServiceImplementation impl = restTestingBase.getServiceImplementation();
        //Package version 1(Initial version)
        PackageItem pkg = impl.getRulesRepository().createPackage( "restPackage1",
                                                                   "this is package restPackage1" );

        //Package version 2	
        DroolsHeader.updateDroolsHeader( "import com.billasurf.Board\n global com.billasurf.Person customer1",
                                         pkg );

        AssetItem func = pkg.addAsset( "func",
                                       "" );
        func.updateFormat( AssetFormats.FUNCTION );
        func.updateContent( "function void foo() { System.out.println(version 1); }" );
        func.checkin( "version 1" );

        AssetItem dsl = pkg.addAsset( "myDSL",
                                      "" );
        dsl.updateFormat( AssetFormats.DSL );
        dsl.updateContent( "[then]call a func=foo();\n[when]foo=FooBarBaz1()" );
        dsl.checkin( "version 1" );

        AssetItem rule = pkg.addAsset( "rule1",
                                       "" );
        rule.updateFormat( AssetFormats.DRL );
        rule.updateContent( "rule 'foo' when Goo1() then end" );
        rule.checkin( "version 1" );

        AssetItem rule2 = pkg.addAsset( "rule2",
                                        "" );
        rule2.updateFormat( AssetFormats.DSL_TEMPLATE_RULE );
        rule2.updateContent( "when \n foo \n then \n call a func" );
        rule2.checkin( "version 1" );

        AssetItem rule3 = pkg.addAsset( "model1",
                                        "" );
        rule3.updateFormat( AssetFormats.DRL_MODEL );
        rule3.updateContent( "declare Album1\n genre1: String \n end" );
        rule3.checkin( "version 1" );

        AssetItem rule4 = pkg.addAsset( "rule4",
                                        "" );
        rule4.updateFormat( AssetFormats.DRL );
        rule4.updateContent( "rule 'nheron' when Goo1() then end" );
        rule4.checkin( "version 1" );
        pkg.checkin( "version2" );

        //Package version 3
        DroolsHeader.updateDroolsHeader( "import com.billasurf.Board\n global com.billasurf.Person customer2",
                                         pkg );
        func.updateContent( "function void foo() { System.out.println(version 2); }" );
        func.checkin( "version 2" );
        dsl.updateContent( "[then]call a func=foo();\n[when]foo=FooBarBaz2()" );
        dsl.checkin( "version 2" );
        rule.updateContent( "rule 'foo' when Goo2() then end" );
        rule.checkin( "version 2" );
        rule2.updateContent( "when \n foo \n then \n call a func" );
        rule2.checkin( "version 2" );
        rule3.updateContent( "declare Album2\n genre2: String \n end" );
        rule3.checkin( "version 2" );
        //impl.buildPackage(pkg.getUUID(), true);
        pkg.checkin( "version3" );               
    }
    
    @AfterClass
    public static void tearDown() {
    	restTestingBase.tearDownGuvnorTestBase();
    }
    
    @Test
    public void testBasicAuthentication() throws MalformedURLException, IOException {
        //Test with invalid user name and pwd
        URL url = new URL(generateBaseUrl() + "/packages");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        String userpassword = "test" + ":" + "invalidPwd";
        byte[] authEncBytes = Base64.encodeBase64(userpassword
                .getBytes());
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        assertEquals (401, connection.getResponseCode());
        //assertEquals(MediaType.APPLICATION_ATOM_XML, connection.getContentType());
        
        //Test with valid user name and pwd
        url = new URL(generateBaseUrl() + "/packages");
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        userpassword = "test" + ":" + "password";
        authEncBytes = Base64.encodeBase64(userpassword
                .getBytes());
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, connection.getContentType());
        //System.out.println(GetContent(connection));        
    }

    /**
     * Test of getPackagesAsFeed method, of class PackageService.
     */
    @Test 
    public void testGetPackagesForJSON() throws MalformedURLException, IOException {
        URL url = new URL(generateBaseUrl() + "/packages");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();        
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
        connection.connect();
        assertEquals (200, connection.getResponseCode());        
        assertEquals(MediaType.APPLICATION_JSON, connection.getContentType());
        //System.out.println(GetContent(connection));
        //TODO: verify
     }

    /**
     * Test of getPackagesAsFeed method, of class PackageService.
     */
    @Test
    public void testGetPackagesForXML() throws MalformedURLException, IOException {
        URL url = new URL(generateBaseUrl() + "/packages");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_XML);
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_XML, connection.getContentType());
        //System.out.println(GetContent(connection));
        //TODO: verify
    }

    /**
     * Test of getPackagesAsFeed method, of class PackageService.
     */
    @Test
    public void testGetPackagesForAtom() throws MalformedURLException, IOException {
        URL url = new URL(generateBaseUrl() + "/packages");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, connection.getContentType());
        //System.out.println(GetContent(connection));
        
        InputStream in = connection.getInputStream();
        assertNotNull(in);
		Document<Feed> doc = abdera.getParser().parse(in);
		Feed feed = doc.getRoot();
		assertEquals("/packages", feed.getBaseUri().getPath());
		assertEquals("Packages", feed.getTitle());
		
		List<Entry> entries = feed.getEntries();
		assertEquals(2, entries.size());
		Iterator<Entry> it = entries.iterator();	
		boolean foundPackageEntry = false;
		while (it.hasNext()) {
			Entry entry = it.next();
			if("restPackage1".equals(entry.getTitle())) {
				foundPackageEntry = true;
				List<Link> links = entry.getLinks();
				assertEquals(1, links.size());
				assertEquals("/packages/restPackage1", links.get(0).getHref().getPath());
			}
		}
		assertTrue(foundPackageEntry);
    }

    /**
     * Test of getPackagesAsFeed method, of class PackageService.
     */
    @Test 
    public void testGetPackageForJSON() throws MalformedURLException, IOException {
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_JSON, connection.getContentType());
        //logger.log (LogLevel, GetContent(connection));
    }

    @Test 
    public void testGetPackageForXML() throws Exception {
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_XML);
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_XML, connection.getContentType());
        //System.out.println(GetContent(connection));
        Package p = unmarshalPackageXML(connection.getInputStream());
        assertEquals("restPackage1", p.getTitle());
        assertEquals("this is package restPackage1", p.getDescription());
        assertEquals("version3", p.getCheckInComment());
        assertEquals(3, p.getVersion());
        assertEquals("http://localhost:9080/packages/restPackage1/source", p.getSourceLink().toString());
        assertEquals("http://localhost:9080/packages/restPackage1/binary", p.getBinaryLink().toString());
        PackageMetadata pm = p.getMetadata();
        assertEquals("alan_parsons", pm.getLastContributor());
        assertNotNull(pm.getCreated());
        assertNotNull(pm.getUuid());
        assertNotNull(pm.getLastModified());
        Set<URI> assetsURI = p.getAssets();
        
        assertEquals(7, assetsURI.size());
    	assertTrue(assetsURI.contains(new URI("http://localhost:9080/packages/restPackage1/assets/drools")));		
    	assertTrue(assetsURI.contains(new URI("http://localhost:9080/packages/restPackage1/assets/func")));		
    	assertTrue(assetsURI.contains(new URI("http://localhost:9080/packages/restPackage1/assets/myDSL")));		
    	assertTrue(assetsURI.contains(new URI("http://localhost:9080/packages/restPackage1/assets/rule1")));		
    	assertTrue(assetsURI.contains(new URI("http://localhost:9080/packages/restPackage1/assets/rule2")));		
    	assertTrue(assetsURI.contains(new URI("http://localhost:9080/packages/restPackage1/assets/rule4")));		
    	assertTrue(assetsURI.contains(new URI("http://localhost:9080/packages/restPackage1/assets/model1")));		
    }

    /**
     * Test of getPackagesAsFeed method, of class PackageService.
     */
    @Test 
    public void testGetPackageForAtom() throws MalformedURLException, IOException {
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, connection.getContentType());
        //System.out.println(GetContent(connection));
        
        InputStream in = connection.getInputStream();
        assertNotNull(in);
		Document<Entry> doc = abdera.getParser().parse(in);
		Entry entry = doc.getRoot();
		assertEquals("/packages/restPackage1", entry.getBaseUri().getPath());		
		assertEquals("restPackage1", entry.getTitle());
		assertNotNull(entry.getPublished());
		assertNotNull(entry.getAuthor().getName());		
		assertEquals("this is package restPackage1", entry.getSummary());
		//assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE.getType(), entry.getContentMimeType().getPrimaryType());
		assertEquals("/packages/restPackage1/binary", entry.getContentSrc().getPath());
		
		List<Link> links = entry.getLinks();
		assertEquals(7, links.size());
		Map<String, Link> linksMap = new HashMap<String, Link>();
		for(Link link : links){
			linksMap.put(link.getTitle(), link);
		}
		
		assertEquals("/packages/restPackage1/assets/drools", linksMap.get("drools").getHref().getPath());		
		assertEquals("/packages/restPackage1/assets/func", linksMap.get("func").getHref().getPath());		
		assertEquals("/packages/restPackage1/assets/myDSL", linksMap.get("myDSL").getHref().getPath());		
		assertEquals("/packages/restPackage1/assets/rule1", linksMap.get("rule1").getHref().getPath());		
		assertEquals("/packages/restPackage1/assets/rule2", linksMap.get("rule2").getHref().getPath());		
		assertEquals("/packages/restPackage1/assets/rule4", linksMap.get("rule4").getHref().getPath());		
		assertEquals("/packages/restPackage1/assets/model1", linksMap.get("model1").getHref().getPath());
		
		ExtensibleElement metadataExtension  = entry.getExtension(Translator.METADATA); 
        ExtensibleElement archivedExtension = metadataExtension.getExtension(Translator.ARCHIVED);     
		assertEquals("false", archivedExtension.getSimpleExtension(Translator.VALUE)); 
        ExtensibleElement uuidExtension = metadataExtension.getExtension(Translator.UUID);     
		assertNotNull(uuidExtension.getSimpleExtension(Translator.VALUE)); 
    }

    /* Package Creation */
    @Test
    public void testCreatePackageFromJAXB() throws Exception {
        Package p = createTestPackage("TestCreatePackageFromJAXB");
        JAXBContext context = JAXBContext.newInstance(p.getClass());
        Marshaller marshaller = context.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(p, sw);
        String xml = sw.toString();
        URL url = new URL(generateBaseUrl() + "/packages");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_XML);
        connection.setRequestProperty("Content-Length", Integer.toString(xml.getBytes().length));
        connection.setUseCaches (false);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        //Send request
        DataOutputStream wr = new DataOutputStream (
              connection.getOutputStream ());
        wr.writeBytes (xml);
        wr.flush ();
        wr.close ();

        assertEquals (200, connection.getResponseCode());
    }

    /* Package Creation */
    @Test  @Ignore
    public void testCreatePackageFromDRLAsEntry() throws Exception {
        URL url = new URL(generateBaseUrl() + "/packages");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.setDoOutput(true);

        //Send request
        BufferedReader br = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("simple_rules.drl")));
        DataOutputStream dos = new DataOutputStream (
              connection.getOutputStream ());
        while (br.ready())
            dos.writeBytes (br.readLine());
        dos.flush();
        dos.close();

        /* Retry with a -1 from the connection */
        if (connection.getResponseCode() == -1) {
            url = new URL(generateBaseUrl() + "/packages");
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
            connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
            connection.setDoOutput(true);

            //Send request
            br = new BufferedReader(new InputStreamReader(
                    getClass().getResourceAsStream("simple_rules.drl")));
            dos = new DataOutputStream (
                  connection.getOutputStream ());
            while (br.ready())
                dos.writeBytes (br.readLine());
            dos.flush();
            dos.close();
        }

        assertEquals (200, connection.getResponseCode());
        assertEquals (MediaType.APPLICATION_ATOM_XML, connection.getContentType());
        //logger.log(LogLevel, GetContent(connection));
    }

    @Test @Ignore
    public void testCreatePackageFromDRLAsJson() throws Exception {
        URL url = new URL(generateBaseUrl() + "/packages");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
        connection.setRequestProperty("Accept", MediaType.APPLICATION_JSON);
        connection.setDoOutput(true);

        //Send request
        BufferedReader br = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("simple_rules2.drl")));
        DataOutputStream dos = new DataOutputStream (
              connection.getOutputStream ());
        while (br.ready())
            dos.writeBytes (br.readLine());
        dos.flush();
        dos.close();

        assertEquals (200, connection.getResponseCode());
        assertEquals (MediaType.APPLICATION_JSON, connection.getContentType());
        //logger.log(LogLevel, GetContent(connection));
    }

    @Test @Ignore
    public void testCreatePackageFromDRLAsJaxB() throws Exception {
        URL url = new URL(generateBaseUrl() + "/packages");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
        connection.setRequestProperty("Accept", MediaType.APPLICATION_XML);
        connection.setDoOutput(true);

        //Send request
        BufferedReader br = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("simple_rules3.drl")));
        DataOutputStream dos = new DataOutputStream (
              connection.getOutputStream ());
        while (br.ready())
            dos.writeBytes (br.readLine());
        dos.flush();
        dos.close();

        assertEquals (200, connection.getResponseCode());
        assertEquals (MediaType.APPLICATION_XML, connection.getContentType());
        //logger.log(LogLevel, GetContent(connection));
    }

    @Test
    public void testCreateAndUpdateAndDeletePackageFromAtom() throws Exception {
    	//Test create
    	Abdera abdera = new Abdera();
    	AbderaClient client = new AbderaClient(abdera);
    	Entry entry = abdera.newEntry();		
    	entry.setTitle("testCreatePackageFromAtom");
    	entry.setSummary("desc for testCreatePackageFromAtom");
    	
    	ClientResponse resp = client.post(generateBaseUrl() + "/packages", entry);
        //System.out.println(GetContent(resp.getInputStream()));

		assertEquals(ResponseType.SUCCESS, resp.getType());

		Document<Entry> doc = resp.getDocument();
		Entry returnedEntry = doc.getRoot();
		assertEquals("/packages/testCreatePackageFromAtom", returnedEntry.getBaseUri().getPath());
		assertEquals("testCreatePackageFromAtom", returnedEntry.getTitle());
		assertEquals("desc for testCreatePackageFromAtom", returnedEntry.getSummary());
		
		//Test update package
        Entry e = abdera.newEntry();
        e.setTitle("testCreatePackageFromAtom");
        org.apache.abdera.model.Link l = Abdera.getNewFactory().newLink();
        l.setHref(generateBaseUrl() + "/packages/" + "testCreatePackageFromAtom");
        l.setRel("self");
        e.addLink(l);
        e.setSummary("updated desc for testCreatePackageFromAtom");
        e.addAuthor("Test McTesty");		
        resp = client.put(generateBaseUrl() + "/packages/testCreatePackageFromAtom", e);
        assertEquals(ResponseType.SUCCESS, resp.getType());
        assertEquals(204, resp.getStatus());

        //NOTE: could not figure out why the code below always returns -1 as the ResponseCode.
/*        URL url = new URL(generateBaseUrl() + "/packages/testCreatePackageFromAtom");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-type", MediaType.APPLICATION_ATOM_XML);
        conn.setRequestProperty("Content-Length", Integer.toString(e.toString().getBytes().length));
        conn.setDoOutput(true);
        e.writeTo(conn.getOutputStream());
        assertEquals(204, conn.getResponseCode());
        conn.disconnect(); */
 
        URL url1 = new URL(generateBaseUrl() + "/packages/testCreatePackageFromAtom");
        HttpURLConnection conn1 = (HttpURLConnection)url1.openConnection();
        conn1.setRequestMethod("GET");
        conn1.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        conn1.connect();
        //System.out.println(GetContent(conn));
        assertEquals (200, conn1.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, conn1.getContentType());
        
        InputStream in = conn1.getInputStream();
        assertNotNull(in);
		doc = abdera.getParser().parse(in);
		entry = doc.getRoot();
		assertEquals("/packages/testCreatePackageFromAtom", entry.getBaseUri().getPath());		
		assertEquals("testCreatePackageFromAtom", entry.getTitle());
		assertTrue(entry.getPublished() != null);
		assertEquals("updated desc for testCreatePackageFromAtom", entry.getSummary());
        
		//Roll back changes. 
		resp = client.delete(generateBaseUrl() + "/packages/testCreatePackageFromAtom");
		assertEquals(ResponseType.SUCCESS, resp.getType());

		//Verify the package is indeed deleted
		URL url2 = new URL(generateBaseUrl() + "/packages/testCreatePackageFromAtom");
		HttpURLConnection conn2 = (HttpURLConnection)url2.openConnection();
        conn2.setRequestMethod("GET");
        conn2.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        conn2.connect();
        //System.out.println(GetContent(connection));
        assertEquals (500, conn2.getResponseCode());
    }

    @Ignore @Test
    public void testCreatePackageFromJson() {
        //TODO: implement test
    }

    @Test
    public void testGetPackageSource() throws Exception {
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/source");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.WILDCARD);
        connection.connect();

        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.TEXT_PLAIN, connection.getContentType());
        String result = GetContent(connection);        
  
        assertEquals("attachment; filename=restPackage1", connection.getHeaderField("Content-Disposition"));
        assertTrue( result.indexOf( "package restPackage1" ) >= 0 );
        assertTrue( result.indexOf( "import com.billasurf.Board" ) >= 0 );
        assertTrue( result.indexOf( "global com.billasurf.Person customer2" ) >= 0 );
        assertTrue( result.indexOf( "function void foo() { System.out.println(version 2); }" ) >= 0 );
        assertTrue( result.indexOf( "declare Album2" ) >= 0 );
    }

    @Test
    @Ignore
    public void testGetPackageBinary () throws Exception {
        /* Tests package compilation in addition to byte retrieval */
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/binary");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_OCTET_STREAM);
        connection.connect();

        assertEquals(200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, connection.getContentType());
        System.out.println(GetContent(connection));
    }

    @Ignore @Test
    public void testUpdatePackageFromJson() {
        //TODO:  implement test
    }

    @Test
    public void testGetPackageVersionsForAtom() throws MalformedURLException, IOException {
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/versions");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, connection.getContentType());
        //System.out.println(GetContent(connection));
        
        InputStream in = connection.getInputStream();
        assertNotNull(in);
		Document<Feed> doc = abdera.getParser().parse(in);
		Feed feed = doc.getRoot();
		assertEquals("Version history of restPackage1", feed.getTitle());
		
		List<Entry> entries = feed.getEntries();
		assertEquals(3, entries.size());

		Map<String, Entry> entriesMap = new HashMap<String, Entry>();
		for(Entry entry : entries){
			entriesMap.put(entry.getTitle(), entry);
		}
		
		assertEquals("/packages/restPackage1/versions/1", entriesMap.get("1").getLinks().get(0).getHref().getPath());		
		assertTrue(entriesMap.get("1").getUpdated() != null);		
		assertEquals("/packages/restPackage1/versions/2", entriesMap.get("2").getLinks().get(0).getHref().getPath());		
		assertTrue(entriesMap.get("2").getUpdated() != null);		
		assertEquals("/packages/restPackage1/versions/3", entriesMap.get("3").getLinks().get(0).getHref().getPath());		
		assertTrue(entriesMap.get("3").getUpdated() != null);		
    }
    
    @Test
    public void testGetHistoricalPackageForAtom() throws MalformedURLException, IOException {
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/versions/2");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.connect();
        assertEquals(200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, connection.getContentType());
        //System.out.println(GetContent(connection));
        
        InputStream in = connection.getInputStream();
        assertNotNull(in);
		Document<Entry> doc = abdera.getParser().parse(in);
		Entry entry = doc.getRoot();
		assertEquals("/packages/restPackage1/versions/2", entry.getBaseUri().getPath());
		assertEquals("restPackage1", entry.getTitle());
		assertEquals("this is package restPackage1", entry.getSummary());
		//assertEquals(MediaType.APPLICATION_OCTET_STREAM_TYPE.getType(), entry.getContentMimeType().getPrimaryType());
		assertEquals("/packages/restPackage1/versions/2/binary", entry.getContentSrc().getPath());
		
		List<Link> links = entry.getLinks();
		assertEquals(7, links.size());
		Map<String, Link> linksMap = new HashMap<String, Link>();
		for(Link link : links){
			linksMap.put(link.getTitle(), link);
		}
		
		assertEquals("/packages/restPackage1/versions/2/assets/drools", linksMap.get("drools").getHref().getPath());		
		assertEquals("/packages/restPackage1/versions/2/assets/func", linksMap.get("func").getHref().getPath());		
		assertEquals("/packages/restPackage1/versions/2/assets/myDSL", linksMap.get("myDSL").getHref().getPath());		
		assertEquals("/packages/restPackage1/versions/2/assets/rule1", linksMap.get("rule1").getHref().getPath());		
		assertEquals("/packages/restPackage1/versions/2/assets/rule2", linksMap.get("rule2").getHref().getPath());		
		assertEquals("/packages/restPackage1/versions/2/assets/model1", linksMap.get("model1").getHref().getPath());   
	}    

    @Test
    public void testGetHistoricalPackageSource() throws Exception {
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/versions/2/source");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.WILDCARD);
        connection.connect();

        assertEquals(200, connection.getResponseCode());
        assertEquals(MediaType.TEXT_PLAIN, connection.getContentType());
        String result = GetContent(connection);
        System.out.println(result);
       
        assertTrue(result.indexOf( "package restPackage1" ) >= 0 );
        assertTrue(result.indexOf( "import com.billasurf.Board" ) >= 0 );
        assertTrue(result.indexOf( "global com.billasurf.Person customer1" ) >= 0 );
        assertTrue(result.indexOf( "function void foo() { System.out.println(version 1); }" ) >= 0 );
        assertTrue(result.indexOf( "declare Album1" ) >= 0 );
    }
    
    @Test 
    public void testGetHistoricalPackageBinary () throws Exception {
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/versions/2/binary");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_OCTET_STREAM);
        connection.connect();

        //TODO:
        //assertEquals (500, connection.getResponseCode());
        //String result = GetContent(connection);
        //System.out.println(result);
    }

    @Test
    public void testUpdateAndGetAssetSource() throws Exception {
        /*
         *  Get the content of rule4
         */
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/rule4/source");
        HttpURLConnection connection1 = (HttpURLConnection) url.openConnection();
        connection1.setRequestMethod("GET");
        connection1.setRequestProperty("Accept", MediaType.TEXT_PLAIN);
        connection1.connect();
        assertEquals(200, connection1.getResponseCode());
        assertEquals(MediaType.TEXT_PLAIN, connection1.getContentType());
        String newContent = "rule 'nheron' when Goo1() then end";
          /*
           * update the content
           */
        URL url2 = new URL(generateBaseUrl() + "/packages/restPackage1/assets/rule4/source");
        HttpURLConnection connection2 = (HttpURLConnection) url2.openConnection();
        connection2.setDoOutput(true);
        connection2.setRequestMethod("PUT");
        connection2.setRequestProperty("Accept", MediaType.APPLICATION_XML);
        OutputStreamWriter out = new OutputStreamWriter(connection2.getOutputStream());
        out.write(newContent);
        out.close();
        connection2.getInputStream();
        assertEquals(204, connection2.getResponseCode());
        /*
         * get the content again and verify it was modified
         */
        URL url3 = new URL(generateBaseUrl() + "/packages/restPackage1/assets/rule4/source");
        HttpURLConnection connection3 = (HttpURLConnection) url3.openConnection();
        connection3.setRequestMethod("GET");
        connection3.setRequestProperty("Accept", MediaType.TEXT_PLAIN);
        connection3.connect();

        assertEquals(200, connection3.getResponseCode());
        assertEquals(MediaType.TEXT_PLAIN, connection3.getContentType());
        String result = GetContent(connection3);
        assertEquals(result,newContent+"\n");
    }
    
    @Test
    public void testCreateAndUpdateAndGetBinaryAsset() throws Exception {      
        //Query if the asset exist
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/Error-image");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        String userpassword = "test" + ":" + "password";
        byte[] authEncBytes = Base64.encodeBase64(userpassword.getBytes());
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        //The asset should not exist
        assertEquals(500, connection.getResponseCode());

        //Create the asset from binary
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets");
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.setRequestProperty("Slug", "Error-image.gif");
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.setDoOutput(true);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[1000];
        int count = 0;
        InputStream is = this.getClass().getResourceAsStream("Error-image.gif");
        while((count = is.read(data,0,1000)) != -1) {
            out.write(data, 0, count);
        }
        connection.getOutputStream ().write(out.toByteArray());
        out.close();
        assertEquals(200, connection.getResponseCode());
        
        //Get the asset meta data and verify
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/Error-image");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        //System.out.println(GetContent(connection));
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, connection.getContentType());
        InputStream in = connection.getInputStream();
        assertNotNull(in);
        Document<Entry> doc = abdera.getParser().parse(in);
        Entry entry = doc.getRoot();
        assertEquals("Error-image", entry.getTitle());
        ExtensibleElement metadataExtension  = entry.getExtension(Translator.METADATA); 
        ExtensibleElement formatExtension = metadataExtension.getExtension(Translator.FORMAT);     
        assertEquals("gif", formatExtension.getSimpleExtension(Translator.VALUE)); 

        assertTrue(entry.getPublished() != null);
        
        //Get the asset binary and verify
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/Error-image/binary");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_OCTET_STREAM);
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        //System.out.println(GetContent(connection));
        assertEquals(200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, connection.getContentType());
        in = connection.getInputStream();
        assertNotNull(in);
                
        //Update asset binary
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/Error-image/binary");
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_XML);
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        byte[] data2 = new byte[1000];
        int count2 = 0;
        InputStream is2 = this.getClass().getResourceAsStream("Error-image-new.gif");
        while((count2 = is2.read(data2,0,1000)) != -1) {
            out2.write(data2, 0, count2);
        }
        connection.getOutputStream ().write(out2.toByteArray());
        out2.close();
        assertEquals(204, connection.getResponseCode());
        
        //Roll back changes. 
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/Error-image");
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        System.out.println(GetContent(connection));
        assertEquals(204, connection.getResponseCode());

        //Verify the package is indeed deleted
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/Error-image");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        assertEquals(500, connection.getResponseCode());
    }
    
    @Test
    public void testGetSourceContentFromBinaryAsset() throws Exception {      
        //Query if the asset exist
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/Error-image-new");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.connect();
        //The asset should not exist
        assertEquals(500, connection.getResponseCode());

        //Create the asset from binary
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets");
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.setRequestProperty("Slug", "Error-image-new");
        connection.setDoOutput(true);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[1000];
        int count = 0;
        InputStream is = this.getClass().getResourceAsStream("Error-image.gif");
        while((count = is.read(data,0,1000)) != -1) {
            out.write(data, 0, count);
        }
        connection.getOutputStream ().write(out.toByteArray());
        out.close();
        assertEquals(200, connection.getResponseCode());
        
        //Get the asset source. this will return the binary data as a byte array.
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/Error-image-new/source");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.TEXT_PLAIN);
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.TEXT_PLAIN, connection.getContentType());
        String result = GetContent(connection);
        assertNotNull(result);
         
        //Roll back changes. 
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/Error-image-new");
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.connect();
        System.out.println(GetContent(connection));
        assertEquals(204, connection.getResponseCode());

        //Verify the package is indeed deleted
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/Error-image-new");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.connect();
        assertEquals(500, connection.getResponseCode());
    }
    
    @Test
    public void testGetBinaryContentFromNonBinaryAsset() throws Exception {    
        //Get the asset binary. If this asset has no binary content, this will return its 
        //source content instead
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/model1/binary");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_OCTET_STREAM);
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, connection.getContentType());
        String result = GetContent(connection);
        assertTrue(result.indexOf("declare Album2") > -1);
    }
    
    @Test
    public void testGetAssetVersionsForAtom() throws MalformedURLException, IOException {
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/model1/versions");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, connection.getContentType());
        //System.out.println(GetContent(connection));
        
        InputStream in = connection.getInputStream();
        assertNotNull(in);
        Document<Feed> doc = abdera.getParser().parse(in);
        Feed feed = doc.getRoot();
        assertEquals("Version history of model1", feed.getTitle());
        
        List<Entry> entries = feed.getEntries();
        assertEquals(2, entries.size());

        Map<String, Entry> entriesMap = new HashMap<String, Entry>();
        for(Entry entry : entries){
            entriesMap.put(entry.getTitle(), entry);
        }
        
        assertEquals("/packages/restPackage1/assets/model1/versions/1", entriesMap.get("1").getLinks().get(0).getHref().getPath());       
        assertTrue(entriesMap.get("1").getUpdated() != null);       
        assertEquals("/packages/restPackage1/assets/model1/versions/2", entriesMap.get("2").getLinks().get(0).getHref().getPath());       
        assertTrue(entriesMap.get("2").getUpdated() != null);     
    }
    
    @Test
    public void testGetHistoricalAssetForAtom() throws MalformedURLException, IOException {
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/model1/versions/1");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, connection.getContentType());
        //System.out.println(GetContent(connection));
        InputStream in = connection.getInputStream();
        
        assertNotNull(in);
        Document<Entry> doc = abdera.getParser().parse(in);
        Entry entry = doc.getRoot();
        assertEquals("model1", entry.getTitle());
        assertTrue(entry.getPublished() != null);        
        assertEquals("/packages/restPackage1/assets/model1/versions/1", entry.getId().getPath());
        assertEquals("/packages/restPackage1/assets/model1/versions/1/binary", entry.getContentSrc().getPath());
        ExtensibleElement metadataExtension  = entry.getExtension(Translator.METADATA); 
        ExtensibleElement formatExtension = metadataExtension.getExtension(Translator.FORMAT);     
        assertEquals("model.drl", formatExtension.getSimpleExtension(Translator.VALUE)); 
        ExtensibleElement stateExtension = metadataExtension.getExtension(Translator.STATE);   
        assertEquals("Draft", stateExtension.getSimpleExtension(Translator.VALUE)); 
        ExtensibleElement archivedExtension = metadataExtension.getExtension(Translator.ARCHIVED);   
        assertEquals("false", archivedExtension.getSimpleExtension(Translator.VALUE)); 
        
        
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/model1/versions/2");
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.connect();
        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, connection.getContentType());
        //System.out.println(GetContent(connection));
        in = connection.getInputStream();
        
        assertNotNull(in);
        doc = abdera.getParser().parse(in);
        entry = doc.getRoot();
        assertEquals("model1", entry.getTitle());
        assertTrue(entry.getPublished() != null);        
        assertEquals("/packages/restPackage1/assets/model1/versions/2", entry.getId().getPath());
        assertEquals("/packages/restPackage1/assets/model1/versions/2/binary", entry.getContentSrc().getPath());
        metadataExtension  = entry.getExtension(Translator.METADATA); 
        formatExtension = metadataExtension.getExtension(Translator.FORMAT);     
        assertEquals("model.drl", formatExtension.getSimpleExtension(Translator.VALUE)); 
        stateExtension = metadataExtension.getExtension(Translator.STATE);   
        assertEquals("Draft", stateExtension.getSimpleExtension(Translator.VALUE)); 
        archivedExtension = metadataExtension.getExtension(Translator.ARCHIVED);   
        assertEquals("false", archivedExtension.getSimpleExtension(Translator.VALUE)); 
  
    }
    
    @Test
    public void testGetHistoricalAssetSource() throws Exception {
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/model1/versions/1/source");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.WILDCARD);
        connection.connect();

        assertEquals(200, connection.getResponseCode());
        assertEquals(MediaType.TEXT_PLAIN, connection.getContentType());
        String result = GetContent(connection);
        System.out.println(result);

        assertTrue(result.indexOf( "declare Album1" ) >= 0 );
        assertTrue(result.indexOf( "genre1: String" ) >= 0 );
        
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/model1/versions/2/source");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.WILDCARD);
        connection.connect();

        assertEquals(200, connection.getResponseCode());
        assertEquals(MediaType.TEXT_PLAIN, connection.getContentType());
        result = GetContent(connection);
        System.out.println(result);

        assertTrue(result.indexOf( "declare Album2" ) >= 0 );
        assertTrue(result.indexOf( "genre2: String" ) >= 0 );
    }  
    
    @Test
    public void testGetHistoricalAssetBinary() throws Exception {
        //Query if the asset exist
        URL url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/testGetHistoricalAssetBinary");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        String userpassword = "test" + ":" + "password";
        byte[] authEncBytes = Base64.encodeBase64(userpassword.getBytes());
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        //The asset should not exist
        assertEquals(500, connection.getResponseCode());

        //Create the asset from binary
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets");
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.setRequestProperty("Slug", "testGetHistoricalAssetBinary.gif");
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.setDoOutput(true);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[1000];
        int count = 0;
        InputStream is = this.getClass().getResourceAsStream("Error-image.gif");
        while((count = is.read(data,0,1000)) != -1) {
            out.write(data, 0, count);
        }
        connection.getOutputStream ().write(out.toByteArray());
        out.close();
        assertEquals(200, connection.getResponseCode());
        
        //Update asset binary
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/testGetHistoricalAssetBinary/binary");
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_XML);
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        byte[] data2 = new byte[1000];
        int count2 = 0;
        InputStream is2 = this.getClass().getResourceAsStream("Error-image-new.gif");
        while((count2 = is2.read(data2,0,1000)) != -1) {
            out2.write(data2, 0, count2);
        }
        connection.getOutputStream ().write(out2.toByteArray());
        out2.close();
        assertEquals(204, connection.getResponseCode());
                
        //Get the asset binary version 1 and verify
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/testGetHistoricalAssetBinary/versions/1/binary");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_OCTET_STREAM);
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        assertEquals(200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, connection.getContentType());
        InputStream in = connection.getInputStream();
        assertNotNull(in);
        
        //Get the asset binary version 2 and verify
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/testGetHistoricalAssetBinary/versions/2/binary");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_OCTET_STREAM);
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        assertEquals(200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, connection.getContentType());
        in = connection.getInputStream();
        assertNotNull(in);
        
        //Roll back changes. 
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/testGetHistoricalAssetBinary");
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("DELETE");
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        System.out.println(GetContent(connection));
        assertEquals(204, connection.getResponseCode());

        //Verify the package is indeed deleted
        url = new URL(generateBaseUrl() + "/packages/restPackage1/assets/testGetHistoricalAssetBinary");
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.connect();
        assertEquals(500, connection.getResponseCode());
    } 
    
    @Test 
    public void testRenamePackageFromAtom() throws Exception {
        //create a package for testing
        Abdera abdera = new Abdera();
        AbderaClient client = new AbderaClient(abdera);
        Entry entry = abdera.newEntry();
        entry.setTitle("testRenamePackageFromAtom");
        entry.setSummary("desc for testRenamePackageFromAtom");
        
        ClientResponse resp = client.post(new URL(generateBaseUrl() + "/packages").toExternalForm(), entry);
        //System.out.println(GetContent(resp.getInputStream()));
        assertEquals(ResponseType.SUCCESS, resp.getType());

        Document<Entry> doc = resp.getDocument();
        Entry returnedEntry = doc.getRoot();
        assertEquals("testRenamePackageFromAtom", returnedEntry.getTitle());
        assertEquals("desc for testRenamePackageFromAtom", returnedEntry.getSummary());
        
        
        //Test rename package
        Entry e = abdera.newEntry();
        e.setTitle("testRenamePackageFromAtomNew");
        org.apache.abdera.model.Link l = Abdera.getNewFactory().newLink();
        l.setHref(new URL(generateBaseUrl() + "/packages/testRenamePackageFromAtomNew").toExternalForm());
        l.setRel("self");
        e.addLink(l);
        e.setSummary("renamed package testRenamePackageFromAtom");
        e.addAuthor("Test McTesty");
        resp = client.put(new URL(generateBaseUrl() + "/packages/testRenamePackageFromAtom").toExternalForm(), e);
        assertEquals(ResponseType.SUCCESS, resp.getType());
        assertEquals(204, resp.getStatus());

        
        //Verify new package is available after renaming
        URL url1 = new URL(generateBaseUrl() + "/packages/testRenamePackageFromAtomNew");
        HttpURLConnection conn1 = (HttpURLConnection)url1.openConnection();
        String userpassword = "test" + ":" + "password";
        byte[] authEncBytes = Base64.encodeBase64(userpassword.getBytes());
        conn1.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        conn1.setRequestMethod("GET");
        conn1.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        conn1.connect();
        //System.out.println(GetContent(conn));
        assertEquals (200, conn1.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, conn1.getContentType());
        
        InputStream in = conn1.getInputStream();
        assertNotNull(in);
        doc = abdera.getParser().parse(in);
        entry = doc.getRoot();
        assertEquals("testRenamePackageFromAtomNew", entry.getTitle());
        assertTrue(entry.getPublished() != null);
        assertEquals("renamed package testRenamePackageFromAtom", entry.getSummary());
        
        
        //Verify the old package does not exist after renaming
        URL url2 = new URL(generateBaseUrl() + "/packages/testRenamePackageFromAtom");
        HttpURLConnection conn2 = (HttpURLConnection)url2.openConnection();
        conn2.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        conn2.setRequestMethod("GET");
        conn2.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        conn2.connect();
        //System.out.println(IOUtils.toString(connection.getInputStream()));
        assertEquals (500, conn2.getResponseCode());
        
        
        //Roll back changes.
        resp = client.delete(new URL(generateBaseUrl() + "/packages/testRenamePackageFromAtomNew").toExternalForm());
        assertEquals(ResponseType.SUCCESS, resp.getType());

        
        //Verify the package is indeed deleted
        URL url3 = new URL(generateBaseUrl() + "/packages/testRenamePackageFromAtomNew");
        HttpURLConnection conn3 = (HttpURLConnection)url3.openConnection();
        conn3.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        conn3.setRequestMethod("GET");
        conn3.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        conn3.connect();
        //System.out.println(IOUtils.toString(connection.getInputStream()));
        assertEquals (500, conn3.getResponseCode());
    }
    
    @Test
    public void testRenamePackageFromXML() throws Exception {
        //create a package for testing
        Package p = createTestPackage("testRenamePackageFromXML");
        p.setDescription("desc for testRenamePackageFromXML");
        JAXBContext context = JAXBContext.newInstance(p.getClass());
        Marshaller marshaller = context.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(p, sw);
        String xml = sw.toString();
        URL url = new URL(generateBaseUrl() + "/packages");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        String userpassword = "test" + ":" + "password";
        byte[] authEncBytes = Base64.encodeBase64(userpassword.getBytes());
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_XML);
        connection.setRequestProperty("Content-Length", Integer.toString(xml.getBytes().length));
        connection.setUseCaches (false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", MediaType.APPLICATION_XML);
        
        DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
        wr.writeBytes (xml);
        wr.flush ();
        wr.close ();

        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_XML, connection.getContentType());
        //System.out.println(IOUtils.toString(connection.getInputStream()));
        Package result = unmarshalPackageXML(connection.getInputStream());
        assertEquals("testRenamePackageFromXML", result.getTitle());
        assertEquals("desc for testRenamePackageFromXML", result.getDescription());
        assertEquals(new URL(generateBaseUrl() + "/packages/testRenamePackageFromXML/source").toExternalForm(), result.getSourceLink().toString());
        assertEquals(new URL(generateBaseUrl() + "/packages/testRenamePackageFromXML/binary").toExternalForm(), result.getBinaryLink().toString());
        PackageMetadata pm = result.getMetadata();
        assertFalse(pm.isArchived());
        assertNotNull(pm.getCreated());
        assertNotNull(pm.getUuid());
        assertNotNull(pm.getLastModified());
        
        
        //Test rename package
        p.setDescription("renamed package testRenamePackageFromXML");
        p.setTitle("testRenamePackageFromXMLNew");
        JAXBContext context2 = JAXBContext.newInstance(p.getClass());
        Marshaller marshaller2 = context2.createMarshaller();
        StringWriter sw2 = new StringWriter();
        marshaller2.marshal(p, sw2);
        String xml2 = sw2.toString();
        URL url2 = new URL(generateBaseUrl() + "/packages/testRenamePackageFromXML");
        HttpURLConnection connection2 = (HttpURLConnection)url2.openConnection();
        connection2.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection2.setRequestMethod("PUT");
        connection2.setRequestProperty("Content-Type", MediaType.APPLICATION_XML);
        connection2.setRequestProperty("Content-Length", Integer.toString(xml2.getBytes().length));
        connection2.setUseCaches (false);
        //connection2.setDoInput(true);
        connection2.setDoOutput(true);

        OutputStreamWriter out = new OutputStreamWriter(connection2.getOutputStream());
        out.write(xml2);
        out.close();
        connection2.getInputStream();
        //assertEquals (200, connection2.getResponseCode());
        
        //Verify the new package is available after renaming
        URL url3 = new URL(generateBaseUrl() + "/packages/testRenamePackageFromXMLNew");
        HttpURLConnection conn3 = (HttpURLConnection)url3.openConnection();
        conn3.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        conn3.setRequestMethod("GET");
        conn3.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        conn3.connect();
        //System.out.println(GetContent(conn));
        assertEquals (200, conn3.getResponseCode());
        assertEquals(MediaType.APPLICATION_ATOM_XML, conn3.getContentType());
        
        InputStream in = conn3.getInputStream();
        assertNotNull(in);
        Document<Entry> doc = abdera.getParser().parse(in);
        Entry entry = doc.getRoot();
        assertEquals("testRenamePackageFromXMLNew", entry.getTitle());
        assertTrue(entry.getPublished() != null);
        assertEquals("renamed package testRenamePackageFromXML", entry.getSummary());
        
        
        //Verify the old package does not exist after renaming
        URL url4 = new URL(generateBaseUrl() + "/packages/testRenamePackageFromXML");
        HttpURLConnection conn4 = (HttpURLConnection)url4.openConnection();
        conn4.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        conn4.setRequestMethod("GET");
        conn4.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        conn4.connect();
        //System.out.println(IOUtils.toString(connection.getInputStream()));
        assertEquals (500, conn4.getResponseCode());
        
        
        //Roll back changes.
        Abdera abdera = new Abdera();
        AbderaClient client = new AbderaClient(abdera);
        ClientResponse resp = client.delete(new URL(generateBaseUrl() + "/packages/testRenamePackageFromXMLNew").toExternalForm());
        assertEquals(ResponseType.SUCCESS, resp.getType());

        
        //Verify the package is indeed deleted
        URL url5 = new URL(generateBaseUrl() + "/packages/testRenamePackageFromXMLNew");
        HttpURLConnection conn5 = (HttpURLConnection)url5.openConnection();
        conn5.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        conn5.setRequestMethod("GET");
        conn5.setRequestProperty("Accept", MediaType.APPLICATION_ATOM_XML);
        conn5.connect();
        //System.out.println(IOUtils.toString(connection.getInputStream()));
        assertEquals (500, conn5.getResponseCode());
    }
    
    @Test
    public void testUpdatePackageFromJAXB() throws Exception {
        //create a package for testing
        Package p = createTestPackage("testUpdatePackageFromJAXB");
        p.setDescription("desc for testUpdatePackageFromJAXB");
        JAXBContext context = JAXBContext.newInstance(p.getClass());
        Marshaller marshaller = context.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(p, sw);
        String xml = sw.toString();
        URL url = new URL(generateBaseUrl() + "/packages");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        String userpassword = "test" + ":" + "password";
        byte[] authEncBytes = Base64.encodeBase64(userpassword.getBytes());
        connection.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", MediaType.APPLICATION_XML);
        connection.setRequestProperty("Content-Length", Integer.toString(xml.getBytes().length));
        connection.setUseCaches (false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", MediaType.APPLICATION_XML);
        
        DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
        wr.writeBytes (xml);
        wr.flush ();
        wr.close ();

        assertEquals (200, connection.getResponseCode());
        assertEquals(MediaType.APPLICATION_XML, connection.getContentType());
        //System.out.println(IOUtils.toString(connection.getInputStream()));
        Package result = unmarshalPackageXML(connection.getInputStream());
        assertEquals("testUpdatePackageFromJAXB", result.getTitle());
        assertEquals("desc for testUpdatePackageFromJAXB", result.getDescription());
        assertEquals(new URL(generateBaseUrl() + "/packages/testUpdatePackageFromJAXB/source").toExternalForm(), result.getSourceLink().toString());
        assertEquals(new URL(generateBaseUrl() + "/packages/testUpdatePackageFromJAXB/binary").toExternalForm(), result.getBinaryLink().toString());
        PackageMetadata pm = result.getMetadata();
        assertFalse(pm.isArchived());
        assertNotNull(pm.getCreated());
        assertNotNull(pm.getUuid());
        assertNotNull(pm.getLastModified());
        
        
        //Test update package
        Package p2 = createTestPackage("testUpdatePackageFromJAXB");
        p2.setDescription("update package testUpdatePackageFromJAXB");
        JAXBContext context2 = JAXBContext.newInstance(p2.getClass());
        Marshaller marshaller2 = context2.createMarshaller();
        StringWriter sw2 = new StringWriter();
        marshaller2.marshal(p2, sw2);
        String xml2 = sw2.toString();
        URL url2 = new URL(generateBaseUrl() + "/packages/testUpdatePackageFromJAXB");
        HttpURLConnection connection2 = (HttpURLConnection)url2.openConnection();
        connection2.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection2.setRequestMethod("PUT");
        connection2.setRequestProperty("Content-Type", MediaType.APPLICATION_XML);
        connection2.setRequestProperty("Content-Length", Integer.toString(xml2.getBytes().length));
        connection2.setUseCaches (false);
        //connection2.setDoInput(true);
        connection2.setDoOutput(true);

        OutputStreamWriter out = new OutputStreamWriter(connection2.getOutputStream());
        out.write(xml2);
        out.close();
        connection2.getInputStream();
       
        
        //Verify
        URL url3 = new URL(generateBaseUrl() + "/packages/testUpdatePackageFromJAXB");
        HttpURLConnection connection3 = (HttpURLConnection)url3.openConnection();
        connection3.setRequestProperty("Authorization", "Basic "
                + new String(authEncBytes));
        connection3.setRequestMethod("GET");
        connection3.setRequestProperty("Accept", MediaType.APPLICATION_XML);
        connection3.connect();
        assertEquals (200, connection3.getResponseCode());
        assertEquals(MediaType.APPLICATION_XML, connection3.getContentType());
        //System.out.println("------------------------");
        //System.out.println(IOUtils.toString(connection.getInputStream()));
        Package p3 = unmarshalPackageXML(connection3.getInputStream());
        assertEquals("testUpdatePackageFromJAXB", p3.getTitle());
        assertEquals("update package testUpdatePackageFromJAXB", p3.getDescription());
        //assertEquals("version3", p3.getCheckInComment());
        assertEquals(new URL(generateBaseUrl() + "/packages/testUpdatePackageFromJAXB/source").toExternalForm(), p3.getSourceLink().toString());
        assertEquals(new URL(generateBaseUrl() + "/packages/testUpdatePackageFromJAXB/binary").toExternalForm(), p3.getBinaryLink().toString());
        PackageMetadata pm3 = p3.getMetadata();
        assertFalse(pm3.isArchived());
        assertNotNull(pm3.getCreated());
        assertNotNull(pm3.getUuid());
        assertNotNull(pm3.getLastModified());
    }  
    
    public String generateBaseUrl() {
    	return "http://localhost:9080";
    }
    public static String GetContent (InputStream is) throws IOException {
        StringAppender ret = new StringAppender();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            ret.append(line + "\n");
        }

        return ret.toString();
    }
    
    public static String GetContent (HttpURLConnection connection) throws IOException {
        StringAppender ret = new StringAppender();
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            ret.append(line + "\n");
        }

        return ret.toString();
    }    

    protected Package createTestPackage(String title) {
        Package p = new Package();
        PackageMetadata metadata = new PackageMetadata();
        metadata.setCreated(new Date(System.currentTimeMillis()));
        metadata.setUuid(UUID.randomUUID().toString());
        metadata.setLastContributor("awaterma");
        metadata.setLastModified(new Date(System.currentTimeMillis()));

        p.setMetadata(metadata);
        p.setCheckInComment("Check in comment for test package.");
        p.setTitle(title);
        p.setDescription("A simple test package with 0 assets.");
        return p;
    }
        
    private Package unmarshalPackageXML(InputStream is) throws Exception {
        JAXBContext c = JAXBContext.newInstance(new Class[]{Package.class});
        Unmarshaller u = c.createUnmarshaller();
        return (Package)u.unmarshal(is);
    }
    
}
