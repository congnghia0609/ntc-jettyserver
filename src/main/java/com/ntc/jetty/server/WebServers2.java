/*
 * Copyright 2015 nghiatc.
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

package com.ntc.jetty.server;

import com.ntc.configer.NConfig;
import com.ntc.jetty.logger.NConsoleJettyLogger;
import com.ntc.jetty.logger.NEmptyJettyLogger;
import com.ntc.jetty.server.ServerCommon.Config;
import com.ntc.jetty.server.ServerCommon.ServerRunner;
import java.net.BindException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nghiatc
 * @since Aug 31, 2015
 */
public class WebServers2 {
	////
	protected String info = null;
	protected QueuedThreadPool threadPool = null;

	private static final Logger logger = LoggerFactory.getLogger(WebServers2.class);
	protected String name = "ntc";
	protected Server server;
	protected Config config = new Config();
	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread thread = null;

	static {
		int jettyLogChannel = NConfig.getConfig().getInt("jettyLogChannel", 0); // 0: off, 1: console, 2: default
		assert (0 <= jettyLogChannel && jettyLogChannel <= 2);

		if (jettyLogChannel == 0) { // off
			org.eclipse.jetty.util.log.Log.setLog(NEmptyJettyLogger.instance);
			System.out.println("Turn off the jetty log!");
		} else if (jettyLogChannel == 1) { // console
			org.eclipse.jetty.util.log.Log.setLog(NConsoleJettyLogger.instance);
			System.out.println("Set the jetty log to console!");
		} // default
	}

	public WebServers2(String name) {
		if (name == null || name.isEmpty()) {
			this.name = "ntc";
		} else {
			this.name = name.trim();
		}
        //===== read config =====
        config.host = NConfig.getConfig().getString(name + ".host", config.host);
        config.port = NConfig.getConfig().getInt(name + ".port", config.port);
        config.acceptQueueSize = NConfig.getConfig().getInt(name + ".acceptQueueSize", config.acceptQueueSize);
        config.nminThreads = NConfig.getConfig().getInt(name + ".nminThreads", config.nminThreads);
        config.nmaxThreads = NConfig.getConfig().getInt(name + ".nmaxThreads", config.nminThreads * 2);
        config.maxIdleTime = NConfig.getConfig().getInt(name + ".maxIdleTime", config.maxIdleTime);
        config.connMaxIdleTime = NConfig.getConfig().getInt(name + ".connMaxIdleTime", config.maxIdleTime);
        config.threadMaxIdleTime = NConfig.getConfig().getInt(name + ".threadMaxIdleTime", config.maxIdleTime);
        config.keyfile = NConfig.getConfig().getString(name + ".keyfile", config.keyfile);
        config.password = NConfig.getConfig().getString(name + ".password", config.password);
        System.out.println("config.keyfile = " + config.keyfile);
        //System.out.println("config.password = " + config.password);
	}
    
    public WebServers2(Config cfg) {
        config = cfg;
	}

	public final String getName() {
		return name;
	}

	public final Server getServer() {
		return server;
	}

	public final Config getConfig() {
		return config.clone();
	}

	public final boolean isRunning() {
		return running.get();
	}

	public final QueuedThreadPool getThreadPool() {
		return threadPool;
	}

	public final int getNWaitingJob() {
		String info = threadPool.toString();
		String sWaitingJob = info.substring(info.lastIndexOf(",") + 1, info.lastIndexOf("}"));
		return Integer.valueOf(sWaitingJob);
	}

	public final String getInfo() {
		return info;
	}

    // https://github.com/tipsy/javalin-http2-example
    // keytool -genkey -alias mydomain -keyalg RSA -keystore jetty.jks -keysize 2048
	public boolean setup(Handler handlers) {
		if (server != null) {
			logger.warn("Server was already setup, dont need to setup again");
			return true;
		}
		try {
			assert (config.isValid());
			
			//===== setup threadPool =====
			threadPool = new QueuedThreadPool();
			threadPool.setName(this.name);
			threadPool.setMinThreads(config.nminThreads);
			threadPool.setMaxThreads(config.nmaxThreads);
			threadPool.setIdleTimeout(config.threadMaxIdleTime);
            
            //create server
			Server server = new Server(threadPool);
            server.manage(threadPool);
            server.setDumpAfterStart(false);
            server.setDumpBeforeStop(false);
            server.setAttribute("threadPool", threadPool);

            // HTTP Configuration
            HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setSendServerVersion(false);
            httpConfig.setSecureScheme("https");
            httpConfig.setSecurePort(config.port);
            
            // SSL Context Factory for HTTPS and HTTP/2
            SslContextFactory sslContextFactory = new SslContextFactory();
            //sslContextFactory.setKeyManagerPassword(config.password); // replace with your real password
            sslContextFactory.setKeyStorePath(config.keyfile); // replace with your real keystore
            sslContextFactory.setKeyStorePassword(config.password); // replace with your real password
            sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
            sslContextFactory.setProvider("Conscrypt");
            
            // HTTPS Configuration
            HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
            httpsConfig.addCustomizer(new SecureRequestCustomizer());
            
            // HTTP/2 Connection Factory
            HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpsConfig);
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
            alpn.setDefaultProtocol("h2");
            
            // SSL Connection Factory
            SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());
            
            //===== HTTP/2 Connector =====
            // Default is non-blocking connection.
            ServerConnector http2Connector = new ServerConnector(server, ssl, alpn, h2, new HttpConnectionFactory(httpsConfig));
            http2Connector.setHost(config.host);
            http2Connector.setPort(config.port);
            http2Connector.setIdleTimeout(config.connMaxIdleTime);
            http2Connector.setAcceptQueueSize(config.acceptQueueSize);
            http2Connector.setName(this.name);
            
            server.addConnector(http2Connector);

			//===== setup server =====
			server.setStopAtShutdown(true);
			if (handlers != null) {
				server.setHandler(handlers);
			}
			//almost done
			this.server = server;

			//===== add info =====
			info = "Server " + name + ": host=" + InetAddress.getByName(config.host).getHostAddress() + ", port=" + config.port
					+ ", " + threadPool.getClass().getName()
					+ "{idleTimeout=" + formatNum(threadPool.getIdleTimeout())
                    + ", nminThreads=" + formatNum(threadPool.getMinThreads())
					+ ", nmaxThreads=" + formatNum(threadPool.getMaxThreads())
					+ "}, " + http2Connector.getClass().getName()
					+ "{acceptQueueSize=" + formatNum(config.acceptQueueSize)
					+ ", connIdleTimeout=" + formatNum(http2Connector.getIdleTimeout())
					+ "}";
			return true;
		} catch (Exception ex) {
			logger.error(null, ex);
			return false;
		}
	}

    public void addConnector(Connector conn){
        if(conn != null){
            server.addConnector(conn);
        }
    }
    
	public boolean start() {
		if (server == null) {
			return false;
		}
		if (!running.compareAndSet(false, true)) {
			logger.warn("Server is already running, dont need to start again");
			return true;
		}
		boolean result = false;
		try {
			server.start();
            ServerRunner sr = new ServerRunner(server, running);
			thread = new Thread(sr, this.name + "-" + System.nanoTime());
			thread.start();
			result = true;
		} catch (BindException ex) {
			logger.error(null, ex);
			stop();
		} catch (Exception ex) {
			logger.error(null, ex);
			stop();
		}
		return result;
	}

	public void stop() {
		if (server == null) {
			return;
		}
		if (running.get()) {
			try {
				server.stop();
				if (thread != null) {
					thread.join();
					thread = null;
				} else {
					running.set(false);
				}
			} catch (Exception ex) {
				logger.error(null, ex);
			}
		}
	}

	public void destroy() {
		if (server == null) {
			return;
		}
		stop();
		server.destroy();
		server = null;
	}
    
    public String formatNum(long l) {
		return String.format("%,d", l);
	}
}
