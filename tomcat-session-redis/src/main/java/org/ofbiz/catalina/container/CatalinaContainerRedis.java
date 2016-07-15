package org.ofbiz.catalina.container;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.catalina.Cluster;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.filters.RequestDumperFilter;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.ContextConfig;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.ofbiz.base.component.ComponentConfig;
import org.ofbiz.base.container.ClassLoaderContainer;
import org.ofbiz.base.container.Container;
import org.ofbiz.base.container.ContainerConfig;
import org.ofbiz.base.container.ContainerConfig.Container.Property;
import org.ofbiz.base.container.ContainerException;
import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.catalina.tomcat.redissessions.RedisSessionHandlerValve;
import org.ofbiz.catalina.tomcat.redissessions.RedisSessionManager;
import org.w3c.dom.Document;

/**
 * CatalinaContainer with redis session management and config redis paramter
 * with cluster properties.
 *
 */
public class CatalinaContainerRedis extends CatalinaContainer implements Container {

	protected boolean useRedis = true;

	protected Cluster createCluster(ContainerConfig.Container.Property clusterProps, Host host)
			throws ContainerException {
		if (useRedis) {
			// nothing, cluster by session redis
			return null;
		}
		return super.createCluster(clusterProps, host);
	}

	protected Callable<Context> createContext(final ComponentConfig.WebappInfo appInfo) throws ContainerException {
		Debug.logInfo("createContext(" + appInfo.name + ")", module);
		final Engine engine = engines.get(appInfo.server);
		if (engine == null) {
			Debug.logWarning("Server with name [" + appInfo.server + "] not found; not mounting [" + appInfo.name + "]",
					module);
			return null;
		}
		List<String> virtualHosts = appInfo.getVirtualHosts();
		final Host host;
		if (UtilValidate.isEmpty(virtualHosts)) {
			host = hosts.get(engine.getName() + "._DEFAULT");
		} else {
			// assume that the first virtual-host will be the default;
			// additional virtual-hosts will be aliases
			Iterator<String> vhi = virtualHosts.iterator();
			String hostName = vhi.next();

			boolean newHost = false;
			if (hosts.containsKey(engine.getName() + "." + hostName)) {
				host = hosts.get(engine.getName() + "." + hostName);
			} else {
				host = createHost(engine, hostName);
				newHost = true;
			}
			while (vhi.hasNext()) {
				host.addAlias(vhi.next());
			}

			if (newHost) {
				hosts.put(engine.getName() + "." + hostName, host);
				engine.addChild(host);
			}
		}

		if (host instanceof StandardHost) {
			// set the catalina's work directory to the host
			StandardHost standardHost = (StandardHost) host;
			standardHost.setWorkDir(new File(System.getProperty(Globals.CATALINA_HOME_PROP),
					"work" + File.separator + engine.getName() + File.separator + host.getName()).getAbsolutePath());
		}

		return new Callable<Context>() {
			public Context call() throws ContainerException, LifecycleException {
				StandardContext context = configureContext(engine, host, appInfo);
				context.setParent(host);
				context.start();
				return context;
			}
		};
	}

	protected StandardContext configureContext(Engine engine, Host host, ComponentConfig.WebappInfo appInfo)
			throws ContainerException {
		// webapp settings
		Map<String, String> initParameters = appInfo.getInitParameters();

		// set the root location (make sure we set the paths correctly)
		String location = appInfo.componentConfig.getRootLocation() + appInfo.location;
		location = location.replace('\\', '/');
		if (location.endsWith("/")) {
			location = location.substring(0, location.length() - 1);
		}

		// get the mount point
		String mount = appInfo.mountPoint;
		if (mount.endsWith("/*")) {
			mount = mount.substring(0, mount.length() - 2);
		}

		final String webXmlFilePath = new StringBuilder().append("file:///").append(location).append("/WEB-INF/web.xml")
				.toString();
		boolean appIsDistributable = distribute;
		URL webXmlUrl = null;
		try {
			webXmlUrl = FlexibleLocation.resolveLocation(webXmlFilePath);
		} catch (MalformedURLException e) {
			throw new ContainerException(e);
		}
		File webXmlFile = new File(webXmlUrl.getFile());
		if (webXmlFile.exists()) {
			Document webXmlDoc = null;
			try {
				webXmlDoc = UtilXml.readXmlDocument(webXmlUrl);
			} catch (Exception e) {
				throw new ContainerException(e);
			}
			appIsDistributable = webXmlDoc.getElementsByTagName("distributable").getLength() > 0;
		} else {
			Debug.logInfo(webXmlFilePath + " not found.", module);
		}
		final boolean contextIsDistributable = distribute && appIsDistributable;

		// configure persistent sessions
		Property clusterProp = clusterConfig.get(engine.getName());

		// redis session manager
		Manager sessionMgr = null;
		Valve sessionValue = null;
		if (useRedis) {
			sessionMgr = new RedisSessionManager();
			sessionValue = new RedisSessionHandlerValve();
			((RedisSessionHandlerValve) sessionValue).setRedisSessionManager((RedisSessionManager) sessionMgr);

			if (clusterProp != null) {
				String redishost = ContainerConfig.getPropertyValue(clusterProp, "redis-host", null);
				if (redishost != null) {
					((RedisSessionManager) sessionMgr).setHost(redishost);
				}
				int redisPort = ContainerConfig.getPropertyValue(clusterProp, "redis-port", 0);
				if (redisPort > 0) {
					((RedisSessionManager) sessionMgr).setPort(redisPort);
				}
				int redisDatabase = ContainerConfig.getPropertyValue(clusterProp, "redis-database", 0);
				if (redisDatabase > 0) {
					((RedisSessionManager) sessionMgr).setDatabase(redisDatabase);
				}
				String redisPassword = ContainerConfig.getPropertyValue(clusterProp, "redis-password", null);
				if (redisPassword != null) {
					((RedisSessionManager) sessionMgr).setPassword(redisPassword);
				}
				int redisTimeout = ContainerConfig.getPropertyValue(clusterProp, "redis-timeout", -1);
				if (redisTimeout >= 0) {
					((RedisSessionManager) sessionMgr).setTimeout(redisTimeout);
				}
				String sentinelMaster = ContainerConfig.getPropertyValue(clusterProp, "redis-sentinelMaster", null);
				if (sentinelMaster != null) {
					((RedisSessionManager) sessionMgr).setSentinelMaster(sentinelMaster);
				}
			}
		} else {
			if (clusterProp != null && contextIsDistributable) {
				String mgrClassName = ContainerConfig.getPropertyValue(clusterProp, "manager-class",
						"org.apache.catalina.ha.session.DeltaManager");
				try {
					sessionMgr = (Manager) Class.forName(mgrClassName).newInstance();
				} catch (Exception exc) {
					throw new ContainerException(
							"Cluster configuration requires a valid manager-class property: " + exc.getMessage());
				}
			} else {
				sessionMgr = new StandardManager();
			}
		}

		// create the web application context
		StandardContext context = new StandardContext();
		context.setParent(host);
		context.setDocBase(location);
		context.setPath(mount);
		context.addLifecycleListener(new ContextConfig());

		JarScanner jarScanner = context.getJarScanner();
		if (jarScanner instanceof StandardJarScanner) {
			StandardJarScanner standardJarScanner = (StandardJarScanner) jarScanner;
			standardJarScanner.setScanClassPath(false);
		}

		Engine egn = (Engine) context.getParent().getParent();
		egn.setService(tomcat.getService());

		Debug.logInfo("host[" + host + "].addChild(" + context + ")", module);
		// context.setDeployOnStartup(false);
		// context.setBackgroundProcessorDelay(5);
		context.setJ2EEApplication(J2EE_APP);
		context.setJ2EEServer(J2EE_SERVER);
		context.setLoader(new WebappLoader(ClassLoaderContainer.getClassLoader()));

		context.setCookies(appInfo.isSessionCookieAccepted());
		context.addParameter("cookies", appInfo.isSessionCookieAccepted() ? "true" : "false");

		context.setDisplayName(appInfo.name);
		context.setDocBase(location);
		context.setAllowLinking(true);

		context.setReloadable(contextReloadable);

		context.setDistributable(contextIsDistributable);

		context.setCrossContext(crossContext);
		context.setPrivileged(appInfo.privileged);

		// for redis
		if (sessionValue != null) {
			context.addValve(sessionValue);
		}
		context.setManager(sessionMgr);
		context.getServletContext().setAttribute("_serverId", appInfo.server);
		context.getServletContext().setAttribute("componentName", appInfo.componentConfig.getComponentName());

		// request dumper filter
		String enableRequestDump = initParameters.get("enableRequestDump");
		if ("true".equals(enableRequestDump)) {
			// create the Requester Dumper Filter instance
			FilterDef requestDumperFilterDef = new FilterDef();
			requestDumperFilterDef.setFilterClass(RequestDumperFilter.class.getName());
			requestDumperFilterDef.setFilterName("RequestDumper");
			FilterMap requestDumperFilterMap = new FilterMap();
			requestDumperFilterMap.setFilterName("RequestDumper");
			requestDumperFilterMap.addURLPattern("*");
			context.addFilterMap(requestDumperFilterMap);
		}

		// create the Default Servlet instance to mount
		StandardWrapper defaultServlet = new StandardWrapper();
		defaultServlet.setParent(context);
		defaultServlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
		defaultServlet.setServletName("default");
		defaultServlet.setLoadOnStartup(1);
		defaultServlet.addInitParameter("debug", "0");
		defaultServlet.addInitParameter("listing", "true");
		defaultServlet.addMapping("/");
		context.addChild(defaultServlet);
		context.addServletMapping("/", "default");

		// create the Jasper Servlet instance to mount
		StandardWrapper jspServlet = new StandardWrapper();
		jspServlet.setParent(context);
		jspServlet.setServletClass("org.apache.jasper.servlet.JspServlet");
		jspServlet.setServletName("jsp");
		jspServlet.setLoadOnStartup(1);
		jspServlet.addInitParameter("fork", "false");
		jspServlet.addInitParameter("xpoweredBy", "true");
		jspServlet.addMapping("*.jsp");
		jspServlet.addMapping("*.jspx");
		context.addChild(jspServlet);
		context.addServletMapping("*.jsp", "jsp");

		// default mime-type mappings
		configureMimeTypes(context);

		// set the init parameters
		for (Map.Entry<String, String> entry : initParameters.entrySet()) {
			context.addParameter(entry.getKey(), entry.getValue());
		}

		context.setRealm(host.getRealm());
		host.addChild(context);
		context.getMapper().setDefaultHostName(host.getName());

		return context;
	}

}
