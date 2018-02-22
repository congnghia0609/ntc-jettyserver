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

package com.ntc.jetty.logger;

import org.eclipse.jetty.util.log.Logger;

/**
 *
 * @author nghiatc
 * @since Aug 31, 2015
 */
public class NConsoleJettyLogger implements org.eclipse.jetty.util.log.Logger {
    public static final NConsoleJettyLogger instance = new NConsoleJettyLogger("NConsoleJettyLogger", false);
	private final String _name;
	private boolean _debugEnable;

	public NConsoleJettyLogger(String name, boolean debugEnable) {
		assert (name != null);
		this._name = name;
		this._debugEnable = debugEnable;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public void warn(Throwable thrwbl) {
		System.out.println("[WARN] " + thrwbl);
	}

	@Override
	public void warn(final String message, Object... os) {
		System.out.println("[WARN] " + message);
	}

	@Override
	public void warn(final String message, final Throwable t) {
		System.out.println("[WARN] " + message + ". Caused by " + t);
	}

	@Override
	public void info(Throwable thrwbl) {
		System.out.println("[INFO] " + thrwbl);
	}

	@Override
	public void info(final String message, Object... os) {
		System.out.println("[INFO] " + message);
	}

	@Override
	public void info(final String message, final Throwable t) {
		System.out.println("[INFO] " + message + ". Caused by " + t);
	}

	@Override
	public void debug(Throwable thrwbl) {
		if (_debugEnable) {
			System.out.println("[DEBUG] " + thrwbl);
		}
	}

	@Override
	public void debug(final String message, Object... os) {
		if (_debugEnable) {
			System.out.println("[DEBUG] " + message);
		}
	}

	@Override
	public void debug(final String message, final Throwable t) {
		if (_debugEnable) {
			System.out.println("[DEBUG] " + message + ". Caused by " + t);
		}
	}
    
    @Override
    public void debug(String message, long l) {
        if (_debugEnable) {
			System.out.println("[DEBUG] " + message + ". " + l);
		}
    }
    
    @Override
	public void ignore(Throwable thrwbl) {
        System.out.println("[IGNORE] " + thrwbl);
	}

	////////////////////////////////////////////////////////////////////////////

	@Override
	public boolean isDebugEnabled() {
		return _debugEnable;
	}

	@Override
	public void setDebugEnabled(boolean bln) {
		if (this == NConsoleJettyLogger.instance) {
			return;//do not allow to change the global instance
		}
		_debugEnable = bln;
	}

	@Override
	public Logger getLogger(String string) {
		return this;
	}

	
}
