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

import com.jmethods.catatumbo.DatastoreKey;
import com.jmethods.catatumbo.Embedded;
import com.jmethods.catatumbo.Entity;
import com.jmethods.catatumbo.EntityConstructor;
import com.jmethods.catatumbo.Exploded;
import com.jmethods.catatumbo.Identifier;
import com.jmethods.catatumbo.Imploded;
import com.jmethods.catatumbo.Key;
import com.jmethods.catatumbo.Property;
import com.jmethods.catatumbo.PropertyOverride;
import com.jmethods.catatumbo.PropertyOverrides;

import java.util.Objects;

/**
 * @author Aurelien Thieriot
 *
 */
@Entity
@PropertyOverrides({
		@PropertyOverride(name = "workAddress.zipCode.fourDigits", property = @Property(name = "zipx", indexed = false)) })
public class ImmutableContact {

	@Identifier
	private long id;

	@Key
	private DatastoreKey key;

	private String firstName;

	private String lastName;

	@Embedded(name = "cellNumber", indexed = true)
	@Imploded
	private ImmutablePhoneNumber mobileNumber;

	@Embedded
	@Imploded
	private Address homeAddress;

	@Embedded
	@Exploded
	private Address workAddress;

	@EntityConstructor
	public ImmutableContact(
			@Property(name = "id") long id,
			@Property(name = "key") DatastoreKey key,
			@Property(name = "firstName") String firstName,
			@Property(name = "lastName") String lastName,
			@Property(name = "mobileNumber") ImmutablePhoneNumber mobileNumber,
			@Property(name = "homeAddress") Address homeAddress,
			@Property(name = "workAddress") Address workAddress
	) {
		this.id = id;
		this.key = key;
		this.firstName = firstName;
		this.lastName = lastName;
		this.mobileNumber = mobileNumber;
		this.homeAddress = homeAddress;
		this.workAddress = workAddress;
	}

	public ImmutableContact(long id, String firstName, String lastName) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public ImmutableContact(long id, String firstName, String lastName, ImmutablePhoneNumber mobileNumber) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.mobileNumber = mobileNumber;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the key
	 */
	public DatastoreKey getKey() {
		return key;
	}

	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * @return the contactNumber
	 */
	public ImmutablePhoneNumber getMobileNumber() {
		return mobileNumber;
	}

	/**
	 * @return the homeAddress
	 */
	public Address getHomeAddress() {
		return homeAddress;
	}

	/**
	 * @return the workAddress
	 */
	public Address getWorkAddress() {
		return workAddress;
	}

	public boolean equalsExceptId(ImmutableContact that) {
		return Objects.equals(this.firstName, that.firstName) && Objects.equals(this.lastName, that.lastName)
				&& Objects.equals(this.mobileNumber, that.mobileNumber)
				&& Objects.equals(this.homeAddress, that.homeAddress)
				&& (Objects.equals(this.workAddress, that.workAddress)
					|| (Objects.isNull(this.workAddress) && that.workAddress.nullified())
					|| (this.workAddress.nullified() && Objects.isNull(that.workAddress)));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ImmutableContact)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		ImmutableContact that = (ImmutableContact) obj;
		return Objects.equals(this.id, that.id) && Objects.equals(this.key, that.key) && this.equalsExceptId(that);
	}

	public static ImmutableContact createContact1() {
		ImmutableContact contact = new ImmutableContact(9000L, "John", "Doe");
		return contact;
	}

	public static ImmutableContact createContact2() {
		ImmutablePhoneNumber phoneNumber = ImmutablePhoneNumber.getSample1();
		return new ImmutableContact(9000L, "John", "Doe", phoneNumber);
	}
}
