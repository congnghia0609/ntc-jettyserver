/*
 * Copyright 2019 nghiatc.
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

import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nghiatc
 * @since Mar 6, 2019
 */
public class ServerCommon {
    private static final Logger logger = LoggerFactory.getLogger(ServerCommon.class);
    
    public static class Config {
		public String host = "0.0.0.0";
		public int port = 80;
		public int acceptQueueSize = 1000;
		public int nminThreads = 100;
		public int nmaxThreads = nminThreads * 2;
		public int maxIdleTime = 60000;
		public int connMaxIdleTime = maxIdleTime;
		public int threadMaxIdleTime = maxIdleTime;
        public String keyfile = "./key/jetty.jks";
        public String password = "test1234";

        public Config() {
        }

        public Config(Config cfg) {
            this.host = cfg.host;
			this.port = cfg.port;
			this.acceptQueueSize = cfg.acceptQueueSize;
			this.nminThreads = cfg.nminThreads;
			this.nmaxThreads = cfg.nmaxThreads;
			this.maxIdleTime = cfg.maxIdleTime;
			this.connMaxIdleTime = cfg.connMaxIdleTime;
			this.threadMaxIdleTime = cfg.threadMaxIdleTime;
            this.keyfile = cfg.keyfile;
			this.password = cfg.password;
        }

		@Override
		public Config clone() {
			Config ret = new Config();
			ret.host = this.host;
			ret.port = this.port;
			ret.acceptQueueSize = this.acceptQueueSize;
			ret.nminThreads = this.nminThreads;
			ret.nmaxThreads = this.nmaxThreads;
			ret.maxIdleTime = this.maxIdleTime;
			ret.connMaxIdleTime = this.connMaxIdleTime;
			ret.threadMaxIdleTime = this.threadMaxIdleTime;
            ret.keyfile = this.keyfile;
			ret.password = this.password;
			return ret;
		}

		public boolean isValid() {
			return !host.isEmpty() && port > 0 && acceptQueueSize > 0
					&& nminThreads > 0 && nmaxThreads >= nminThreads
					&& connMaxIdleTime > 0 && threadMaxIdleTime > 0;
		}

        @Override
        public String toString() {
            return "Config{" + "host=" + host + ", port=" + port
                    + ", acceptQueueSize=" + acceptQueueSize + ", nminThreads=" + nminThreads 
                    + ", nmaxThreads=" + nmaxThreads + ", maxIdleTime=" + maxIdleTime 
                    + ", connMaxIdleTime=" + connMaxIdleTime + ", threadMaxIdleTime=" + threadMaxIdleTime
                    + ", key=" + keyfile + ", pass=" + password
                    + '}';
        }
	}

	public static class ServerRunner implements Runnable {

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
	};
}
