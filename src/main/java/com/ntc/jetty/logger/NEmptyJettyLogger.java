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
public class NEmptyJettyLogger implements org.eclipse.jetty.util.log.Logger {
    public static final NEmptyJettyLogger instance = new NEmptyJettyLogger("NEmptyJettyLogger");
	private final String _name;

	private NEmptyJettyLogger(String name) {
		assert (name != null);
		this._name = name;
	}

	@Override
	public String getName() {
		return _name;
	}

	@Override
	public void warn(String string, Object... os) {
	}

	@Override
	public void warn(Throwable thrwbl) {
	}

	@Override
	public void warn(String string, Throwable thrwbl) {
	}

	@Override
	public void info(String string, Object... os) {
	}

	@Override
	public void info(Throwable thrwbl) {
	}

	@Override
	public void info(String string, Throwable thrwbl) {
	}

	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public void setDebugEnabled(boolean bln) {
	}

	@Override
	public void debug(String string, Object... os) {
	}

	@Override
	public void debug(Throwable thrwbl) {
	}

	@Override
	public void debug(String string, Throwable thrwbl) {
	}

	@Override
	public Logger getLogger(String string) {
		return this;
	}

	@Override
	public void ignore(Throwable thrwbl) {
	}

    @Override
    public void debug(String string, long l) {
    }
}
