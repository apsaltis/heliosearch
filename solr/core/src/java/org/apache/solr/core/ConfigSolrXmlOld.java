package org.apache.solr.core;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.solr.common.SolrException;
import org.apache.solr.util.DOMUtil;
import org.apache.solr.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 *
 */
public class ConfigSolrXmlOld extends ConfigSolr {

  protected static Logger log = LoggerFactory.getLogger(ConfigSolrXmlOld.class);

  private NodeList coreNodes = null;
  
  private final CoresLocator persistor;

  @Override
  protected String getShardHandlerFactoryConfigPath() {
    return "solr/cores/shardHandlerFactory";
  }

  public ConfigSolrXmlOld(Config config, String originalXML) {
    super(config);
    try {
      checkForIllegalConfig();
      fillPropMap();
      config.substituteProperties();
      initCoreList();
      this.persistor = isPersistent() ? new SolrXMLCoresLocator(originalXML, this)
                                      : new SolrXMLCoresLocator.NonPersistingLocator(originalXML, this);
    }
    catch (IOException e) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, e);
    }
  }

  @Override
  public CoresLocator getCoresLocator() {
    return this.persistor;
  }
  
  private void checkForIllegalConfig() throws IOException {
    // Do sanity checks - we don't want to find new style
    // config
    failIfFound("solr/str[@name='adminHandler']");
    failIfFound("solr/int[@name='coreLoadThreads']");
    failIfFound("solr/str[@name='coreRootDirectory']");
    failIfFound("solr/solrcloud/int[@name='distribUpdateConnTimeout']");
    failIfFound("solr/solrcloud/int[@name='distribUpdateSoTimeout']");
    failIfFound("solr/solrcloud/str[@name='host']");
    failIfFound("solr/solrcloud/str[@name='hostContext']");
    failIfFound("solr/solrcloud/int[@name='hostPort']");
    failIfFound("solr/solrcloud/int[@name='leaderVoteWait']");
    failIfFound("solr/solrcloud/int[@name='genericCoreNodeNames']");
    failIfFound("solr/str[@name='managementPath']");
    failIfFound("solr/str[@name='sharedLib']");
    failIfFound("solr/str[@name='shareSchema']");
    failIfFound("solr/int[@name='transientCacheSize']");
    failIfFound("solr/solrcloud/int[@name='zkClientTimeout']");
    failIfFound("solr/solrcloud/int[@name='zkHost']");
    
    failIfFound("solr/logging/str[@name='class']");
    failIfFound("solr/logging/str[@name='enabled']");
    
    failIfFound("solr/logging/watcher/int[@name='size']");
    failIfFound("solr/logging/watcher/int[@name='threshold']");
  }
  
  private void failIfFound(String xPath) {

    if (config.getVal(xPath, false) != null) {
      throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Should not have found " + xPath +
          " solr.xml may be a mix of old and new style formats.");
    }
  }

  @Override
  public boolean isPersistent() {
    return config.getBool("solr/@persistent", false);
  }

  @Override
  public String getDefaultCoreName() {
    return get(CfgProp.SOLR_CORES_DEFAULT_CORE_NAME, DEFAULT_DEFAULT_CORE_NAME);
  }
  
  private void fillPropMap() {
    
    propMap.put(CfgProp.SOLR_CORELOADTHREADS,
        config.getVal("solr/@coreLoadThreads", false));
    propMap
        .put(CfgProp.SOLR_SHAREDLIB, config.getVal("solr/@sharedLib", false));
    propMap.put(CfgProp.SOLR_ZKHOST, config.getVal("solr/@zkHost", false));
    
    propMap.put(CfgProp.SOLR_LOGGING_CLASS,
        config.getVal("solr/logging/@class", false));
    propMap.put(CfgProp.SOLR_LOGGING_ENABLED,
        config.getVal("solr/logging/@enabled", false));
    propMap.put(CfgProp.SOLR_LOGGING_WATCHER_SIZE,
        config.getVal("solr/logging/watcher/@size", false));
    propMap.put(CfgProp.SOLR_LOGGING_WATCHER_THRESHOLD,
        config.getVal("solr/logging/watcher/@threshold", false));
    
    propMap.put(CfgProp.SOLR_ADMINHANDLER,
        config.getVal("solr/cores/@adminHandler", false));
    propMap.put(CfgProp.SOLR_COLLECTIONSHANDLER, config.getVal("solr/cores/@collectionsHandler", false));
    propMap.put(CfgProp.SOLR_INFOHANDLER, config.getVal("solr/cores/@infoHandler", false));
    propMap.put(CfgProp.SOLR_DISTRIBUPDATECONNTIMEOUT,
        config.getVal("solr/cores/@distribUpdateConnTimeout", false));
    propMap.put(CfgProp.SOLR_DISTRIBUPDATESOTIMEOUT,
        config.getVal("solr/cores/@distribUpdateSoTimeout", false));
    propMap.put(CfgProp.SOLR_MAXUPDATECONNECTIONS,
        config.getVal("solr/cores/@maxUpdateConnections", false));
    propMap.put(CfgProp.SOLR_MAXUPDATECONNECTIONSPERHOST,
        config.getVal("solr/cores/@maxUpdateConnectionsPerHost", false));
    propMap.put(CfgProp.SOLR_HOST, config.getVal("solr/cores/@host", false));
    propMap.put(CfgProp.SOLR_HOSTCONTEXT,
        config.getVal("solr/cores/@hostContext", false));
    propMap.put(CfgProp.SOLR_HOSTPORT,
        config.getVal("solr/cores/@hostPort", false));
    propMap.put(CfgProp.SOLR_LEADERVOTEWAIT,
        config.getVal("solr/cores/@leaderVoteWait", false));
    propMap.put(CfgProp.SOLR_GENERICCORENODENAMES,
        config.getVal("solr/cores/@genericCoreNodeNames", false));
    propMap.put(CfgProp.SOLR_MANAGEMENTPATH,
        config.getVal("solr/cores/@managementPath", false));
    propMap.put(CfgProp.SOLR_SHARESCHEMA,
        config.getVal("solr/cores/@shareSchema", false));
    propMap.put(CfgProp.SOLR_TRANSIENTCACHESIZE,
        config.getVal("solr/cores/@transientCacheSize", false));
    propMap.put(CfgProp.SOLR_ZKCLIENTTIMEOUT,
        config.getVal("solr/cores/@zkClientTimeout", false));

    // These have no counterpart in 5.0, asking, for any of these in Solr 5.0
    // will result in an error being
    // thrown.
    propMap.put(CfgProp.SOLR_CORES_DEFAULT_CORE_NAME,
        config.getVal("solr/cores/@defaultCoreName", false));
    propMap.put(CfgProp.SOLR_PERSISTENT,
        config.getVal("solr/@persistent", false));
    propMap.put(CfgProp.SOLR_ADMINPATH,
        config.getVal("solr/cores/@adminPath", false));
    
  }

  private void initCoreList() throws IOException {
    
    coreNodes = (NodeList) config.evaluate("solr/cores/core",
        XPathConstants.NODESET);
    // Check a couple of error conditions
    Set<String> names = new HashSet<>(); // for duplicate names
    Map<String,String> dirs = new HashMap<>(); // for duplicate
                                                            // data dirs.
    
    for (int idx = 0; idx < coreNodes.getLength(); ++idx) {
      Node node = coreNodes.item(idx);
      String name = DOMUtil.getAttr(node, CoreDescriptor.CORE_NAME, null);

      String dataDir = DOMUtil.getAttr(node, CoreDescriptor.CORE_DATADIR, null);
      if (dataDir != null) dataDir = PropertiesUtil.substituteProperty(dataDir, null);
      if (name != null) {
        if (!names.contains(name)) {
          names.add(name);
        } else {
          String msg = String.format(Locale.ROOT,
              "More than one core defined for core named %s", name);
          log.error(msg);
        }
      }

      String instDir = DOMUtil.getAttr(node, CoreDescriptor.CORE_INSTDIR, null);
      if (instDir != null) instDir = PropertiesUtil.substituteProperty(instDir, null);

      if (dataDir != null) {
        String absData = null;
        File dataFile = new File(dataDir);
        if (dataFile.isAbsolute()) {
          absData = dataFile.getCanonicalPath();
        } else if (instDir != null) {
          File instFile = new File(instDir);
          absData = new File(instFile, dataDir).getCanonicalPath();
        }
        if (absData != null) {
          if (!dirs.containsKey(absData)) {
            dirs.put(absData, name);
          } else {
            String msg = String
                .format(
                    Locale.ROOT,
                    "More than one core points to data dir %s. They are in %s and %s",
                    absData, dirs.get(absData), name);
            log.warn(msg);
          }
        }
      }
    }
    
  }

  public List<String> getAllCoreNames() {
    List<String> ret = new ArrayList<>();
    
    synchronized (coreNodes) {
      for (int idx = 0; idx < coreNodes.getLength(); ++idx) {
        Node node = coreNodes.item(idx);
        ret.add(DOMUtil.getAttr(node, CoreDescriptor.CORE_NAME, null));
      }
    }
    
    return ret;
  }

  public String getProperty(String coreName, String property, String defaultVal) {
    
    synchronized (coreNodes) {
      for (int idx = 0; idx < coreNodes.getLength(); ++idx) {
        Node node = coreNodes.item(idx);
        if (coreName.equals(DOMUtil.getAttr(node, CoreDescriptor.CORE_NAME,
            null))) {
          String propVal = DOMUtil.getAttr(node, property);
          if (propVal == null)
            propVal = defaultVal;
          return PropertiesUtil.substituteProperty(propVal, null);
        }
      }
    }
    return defaultVal;
    
  }

  public Properties getCoreProperties(String coreName) {
    synchronized (coreNodes) {
      for (int idx = 0; idx < coreNodes.getLength(); idx++) {
        Node node = coreNodes.item(idx);
        if (coreName.equals(DOMUtil.getAttr(node, CoreDescriptor.CORE_NAME, null))) {
          try {
            return readProperties(node);
          } catch (XPathExpressionException e) {
            SolrException.log(log, e);
          }
        }
      }
    }
    return new Properties();
  }

  public static final String DEFAULT_DEFAULT_CORE_NAME = "collection1";

  public static final String DEF_SOLR_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n"
        + "<solr persistent=\"false\">\n"
        + "  <cores adminPath=\"/admin/cores\" defaultCoreName=\""
        + DEFAULT_DEFAULT_CORE_NAME
        + "\""
        + " host=\"${host:}\" hostPort=\"${hostPort:}\" hostContext=\"${hostContext:}\" zkClientTimeout=\"${zkClientTimeout:15000}\""
        + ">\n"
        + "    <core name=\""
        + DEFAULT_DEFAULT_CORE_NAME
        + "\" shard=\"${shard:}\" collection=\"${collection:collection1}\" instanceDir=\"collection1\" />\n"
        + "  </cores>\n" + "</solr>";

}
