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
import java.net.BindException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nghiatc
 * @since Aug 31, 2015
 */
public class WebServers {
	////
	protected String info = null;
	protected QueuedThreadPool threadPool = null;

	private static final Logger logger = LoggerFactory.getLogger(WebServers.class);
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

	public static class Config {
		public String host = "0.0.0.0";
		public int port = 80;
		public int nconnectors = 1;
		public int acceptQueueSize = 500;
		public int nminThreads = 100;
		public int nmaxThreads = nminThreads * 2;
		public int maxIdleTime = 60000;
		public int connMaxIdleTime = maxIdleTime;
		public int threadMaxIdleTime = maxIdleTime;

        public Config() {
        }

        public Config(Config cfg) {
            this.host = cfg.host;
			this.port = cfg.port;
			this.nconnectors = cfg.nconnectors;
			this.acceptQueueSize = cfg.acceptQueueSize;
			this.nminThreads = cfg.nminThreads;
			this.nmaxThreads = cfg.nmaxThreads;
			this.maxIdleTime = cfg.maxIdleTime;
			this.connMaxIdleTime = cfg.connMaxIdleTime;
			this.threadMaxIdleTime = cfg.threadMaxIdleTime;
        }

		@Override
		public Config clone() {
			Config ret = new Config();
			ret.host = this.host;
			ret.port = this.port;
			ret.nconnectors = this.nconnectors;
			ret.acceptQueueSize = this.acceptQueueSize;
			ret.nminThreads = this.nminThreads;
			ret.nmaxThreads = this.nmaxThreads;
			ret.maxIdleTime = this.maxIdleTime;
			ret.connMaxIdleTime = this.connMaxIdleTime;
			ret.threadMaxIdleTime = this.threadMaxIdleTime;
			return ret;
		}

		public boolean isValid() {
			return !host.isEmpty() && port > 0 && nconnectors > 0 && acceptQueueSize > 0
					&& nminThreads > 0 && nmaxThreads >= nminThreads
					&& connMaxIdleTime > 0 && threadMaxIdleTime > 0;
		}

        @Override
        public String toString() {
            return "Config{" + "host=" + host + ", port=" + port + ", nconnectors=" + nconnectors 
                    + ", acceptQueueSize=" + acceptQueueSize + ", nminThreads=" + nminThreads 
                    + ", nmaxThreads=" + nmaxThreads + ", maxIdleTime=" + maxIdleTime 
                    + ", connMaxIdleTime=" + connMaxIdleTime + ", threadMaxIdleTime=" + threadMaxIdleTime + '}';
        }
	}

	protected class ServerRunner implements Runnable {

		private final Server server;
		private final AtomicBoolean running;

		public ServerRunner(Server server, AtomicBoolean running) {
			assert (server != null && running != null);
			this.server = server;
			this.running = running;
		}

		@Override
		public void run() {
			logger.info("Web server is going to serve");
			try {
				server.join();
			} catch (Exception ex) {
				logger.error(null, ex);
			}
			logger.info("Web server stopped");
			running.set(false);
		}
	}

	public WebServers(String name) {
		if (name == null || name.isEmpty()) {
			this.name = "ntc";
		} else {
			this.name = name.trim();
		}
        //===== read config =====
        config.host = NConfig.getConfig().getString(name + ".host", config.host);
        config.port = NConfig.getConfig().getInt(name + ".port", config.port);
        config.nconnectors = NConfig.getConfig().getInt(name + ".nconnectors", config.nconnectors);
        config.acceptQueueSize = NConfig.getConfig().getInt(name + ".acceptQueueSize", config.acceptQueueSize);
        config.nminThreads = NConfig.getConfig().getInt(name + ".nminThreads", config.nminThreads);
        config.nmaxThreads = NConfig.getConfig().getInt(name + ".nmaxThreads", config.nminThreads * 2);
        config.maxIdleTime = NConfig.getConfig().getInt(name + ".maxIdleTime", config.maxIdleTime);
        config.connMaxIdleTime = NConfig.getConfig().getInt(name + ".connMaxIdleTime", config.maxIdleTime);
        config.threadMaxIdleTime = NConfig.getConfig().getInt(name + ".threadMaxIdleTime", config.maxIdleTime);
	}
    
    public WebServers(Config cfg) {
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

			//===== setup connector[] =====
            StringBuffer portsBuffer = new StringBuffer();
			Connector[] connectors = new Connector[config.nconnectors];
			for (int i = 0; i < config.nconnectors; ++i) {
                ServerConnector conn = new ServerConnector(server); // Default is non-blocking connection.
                conn.setHost(config.host);
                conn.setPort(config.port + i);
                conn.setIdleTimeout(config.connMaxIdleTime);
                conn.setAcceptQueueSize(config.acceptQueueSize);
                conn.setName(this.name + i);
                connectors[i] = conn;
                
				if (i != 0) {
					portsBuffer.append(",");
				}
				portsBuffer.append(config.port + i);
			}
			server.setConnectors(connectors);
			String ports = portsBuffer.toString();

			//===== setup server =====
			server.setStopAtShutdown(true);
			if (handlers != null) {
				server.setHandler(handlers);
			}
			//almost done
			this.server = server;

			//===== add info =====
			info = "Server " + name + ": host=" + InetAddress.getByName(config.host).getHostAddress() + ", port=" + ports
					+ ", " + threadPool.getClass().getName()
					+ "{idleTimeout=" + formatNum(threadPool.getIdleTimeout())
                    + ", nminThreads=" + formatNum(threadPool.getMinThreads())
					+ ", nmaxThreads=" + formatNum(threadPool.getMaxThreads())
					+ "}, " + connectors[0].getClass().getName()
					+ "{acceptQueueSize=" + formatNum(config.acceptQueueSize)
					+ ", connIdleTimeout=" + formatNum(connectors[0].getIdleTimeout())
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
			thread = new Thread(new ServerRunner(server, running), this.name + "-" + System.nanoTime());
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
		if (-1000 < l && l < 1000) {
			return Long.toString(l);
		}
		// some common case
		if (l == 3600) { // default exp time
			return "3,600";
		}
		if (l == 5000) { // default timeout
			return "5,000";
		}
		if (l == 1000000) { // default queue size, lru size
			return "1,000,000";
		}
		return String.format("%,d", l);
	}
}
