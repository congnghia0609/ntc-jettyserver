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

	public WebServers(String name) {
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

			//===== setup connector =====
            ServerConnector conn = new ServerConnector(server); // Default is non-blocking connection.
            conn.setHost(config.host);
            conn.setPort(config.port);
            conn.setIdleTimeout(config.connMaxIdleTime);
            conn.setAcceptQueueSize(config.acceptQueueSize);
            conn.setName(this.name);

			server.addConnector(conn);

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
					+ "}, " + conn.getClass().getName()
					+ "{acceptQueueSize=" + formatNum(config.acceptQueueSize)
					+ ", connIdleTimeout=" + formatNum(conn.getIdleTimeout())
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
