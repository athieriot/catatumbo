/*
 * Copyright 2016 Sai Pullabhotla.
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

package com.jmethods.catatumbo.entities;

import com.jmethods.catatumbo.Embeddable;
import com.jmethods.catatumbo.EntityConstructor;
import com.jmethods.catatumbo.Property;

import java.util.Objects;

/**
 * @author Sai Pullabhotla
 *
 */
@Embeddable
public class ImmutablePhoneNumber {

	@Property(optional = true)
	private final String countryCode;

	private final String areaCode;

	private final String subscriberNumber;

	@EntityConstructor
	public ImmutablePhoneNumber(
			@Property(name = "countryCode") String countryCode,
			@Property(name = "areaCode") String areaCode,
			@Property(name = "subscriberNumber") String subscriberNumber
	) {
		this.countryCode = countryCode;
		this.areaCode = areaCode;
		this.subscriberNumber = subscriberNumber;
	}

	/**
	 * @return the countryCode
	 */
	public String getCountryCode() {
		return countryCode;
	}

	/**
	 * @return the areaCode
	 */
	public String getAreaCode() {
		return areaCode;
	}

	/**
	 * @return the number
	 */
	public String getSubscriberNumber() {
		return subscriberNumber;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "+" + countryCode + " (" + areaCode + ")" + subscriberNumber;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override

	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ImmutablePhoneNumber)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		ImmutablePhoneNumber that = (ImmutablePhoneNumber) obj;
		return Objects.equals(this.countryCode, that.countryCode) && Objects.equals(this.areaCode, that.areaCode)
				&& Objects.equals(this.subscriberNumber, that.subscriberNumber);
	}

	@Override
	public int hashCode() {
		return Objects.hash(countryCode, areaCode, subscriberNumber);
	}

	public static ImmutablePhoneNumber getSample1() {
		return new ImmutablePhoneNumber(
				"1",
				"212",
				"8889999"
		);
	}

	public static ImmutablePhoneNumber getSample2() {
		return new ImmutablePhoneNumber(
				"1",
				"402",
				"5556666"
		);
	}

	public static ImmutablePhoneNumber getSample3() {
		return new ImmutablePhoneNumber(
				"91",
				"40",
				"2722 5858"
		);
	}

	public static ImmutablePhoneNumber getSample4() {
		return new ImmutablePhoneNumber(
				"91",
				"80",
				"6666 0000"
		);
	}

}
