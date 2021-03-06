/**
 * This file is part of d:swarm graph extension.
 *
 * d:swarm graph extension is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * d:swarm graph extension is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with d:swarm graph extension.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dswarm.graph.model;

import java.util.Map;

import org.dswarm.graph.NodeType;

import java.util.Optional;

/**
 * @author tgaengler
 */
public final class StatementBuilder {

	private Optional<NodeType>            optionalSubjectNodeType     = Optional.empty();
	private Optional<String>              optionalSubjectURI          = Optional.empty();
	private Optional<String>              optionalSubjectId           = Optional.empty();
	private Optional<String>              optionalSubjectDataModelURI = Optional.empty();
	private Optional<String>              optionalPredicateURI        = Optional.empty();
	private Optional<NodeType>            optionalObjectNodeType      = Optional.empty();
	private Optional<String>              optionalObjectURI           = Optional.empty();
	private Optional<String>              optionalObjectValue         = Optional.empty();
	private Optional<String>              optionalObjectId            = Optional.empty();
	private Optional<String>              optionalObjectDataModelURI  = Optional.empty();
	private Optional<String>              optionalStatementUUID       = Optional.empty();
	private Optional<Long>                optionalResourceHash        = Optional.empty();
	private Optional<Map<String, Object>> optionalQualifiedAttributes = Optional.empty();

	public StatementBuilder() {
	}

	public StatementBuilder setOptionalSubjectNodeType(final Optional<NodeType> optionalSubjectNodeType) {

		this.optionalSubjectNodeType = optionalSubjectNodeType;

		return this;
	}

	public StatementBuilder setOptionalSubjectURI(final Optional<String> optionalSubjectURI) {

		this.optionalSubjectURI = optionalSubjectURI;

		return this;
	}

	public StatementBuilder setOptionalSubjectId(final Optional<String> optionalSubjectId) {

		this.optionalSubjectId = optionalSubjectId;

		return this;
	}

	public StatementBuilder setOptionalSubjectDataModelURI(final Optional<String> optionalSubjectDataModelURI) {

		this.optionalSubjectDataModelURI = optionalSubjectDataModelURI;

		return this;
	}

	public StatementBuilder setOptionalPredicateURI(final Optional<String> optionalPredicateURI) {

		this.optionalPredicateURI = optionalPredicateURI;

		return this;
	}

	public StatementBuilder setOptionalObjectNodeType(final Optional<NodeType> optionalObjectNodeType) {

		this.optionalObjectNodeType = optionalObjectNodeType;

		return this;
	}

	public StatementBuilder setOptionalObjectURI(final Optional<String> optionalObjectURI) {

		this.optionalObjectURI = optionalObjectURI;

		return this;
	}

	public StatementBuilder setOptionalObjectValue(final Optional<String> optionalObjectValue) {

		this.optionalObjectValue = optionalObjectValue;

		return this;
	}

	public StatementBuilder setOptionalObjectId(final Optional<String> optionalObjectId) {

		this.optionalObjectId = optionalObjectId;

		return this;
	}

	public StatementBuilder setOptionalObjectDataModelURI(final Optional<String> optionalObjectDataModelURI) {

		this.optionalObjectDataModelURI = optionalObjectDataModelURI;

		return this;
	}

	public StatementBuilder setOptionalStatementUUID(final Optional<String> optionalStatementUUID) {

		this.optionalStatementUUID = optionalStatementUUID;

		return this;
	}

	public StatementBuilder setOptionalResourceHash(final Optional<Long> optionalResourceHash) {

		this.optionalResourceHash = optionalResourceHash;

		return this;
	}

	public StatementBuilder setOptionalQualifiedAttributes(final Optional<Map<String, Object>> optionalQualifiedAttributes) {

		this.optionalQualifiedAttributes = optionalQualifiedAttributes;

		return this;
	}

	public Optional<NodeType> getOptionalSubjectNodeType() {

		if (optionalSubjectNodeType == null) {

			return Optional.empty();
		}

		return optionalSubjectNodeType;
	}

	public Optional<String> getOptionalSubjectURI() {

		if (optionalSubjectURI == null) {

			return Optional.empty();
		}

		return optionalSubjectURI;
	}

	public Optional<String> getOptionalSubjectId() {

		if (optionalSubjectId == null) {

			return Optional.empty();
		}

		return optionalSubjectId;
	}

	public Optional<String> getOptionalSubjectDataModelURI() {

		if (optionalSubjectDataModelURI == null) {

			return Optional.empty();
		}

		return optionalSubjectDataModelURI;
	}

	public Optional<String> getOptionalPredicateURI() {

		if (optionalPredicateURI == null) {

			return Optional.empty();
		}

		return optionalPredicateURI;
	}

	public Optional<NodeType> getOptionalObjectNodeType() {

		if (optionalObjectNodeType == null) {

			return Optional.empty();
		}

		return optionalObjectNodeType;
	}

	public Optional<String> getOptionalObjectURI() {

		if (optionalObjectURI == null) {

			return Optional.empty();
		}

		return optionalObjectURI;
	}

	public Optional<String> getOptionalObjectValue() {

		if (optionalObjectValue == null) {

			return Optional.empty();
		}

		return optionalObjectValue;
	}

	public Optional<String> getOptionalObjectId() {

		if (optionalObjectId == null) {

			return Optional.empty();
		}

		return optionalObjectId;
	}

	public Optional<String> getOptionalObjectDataModelURI() {

		if (optionalObjectDataModelURI == null) {

			return Optional.empty();
		}

		return optionalObjectDataModelURI;
	}

	public Optional<String> getOptionalStatementUUID() {

		if (optionalStatementUUID == null) {

			return Optional.empty();
		}

		return optionalStatementUUID;
	}

	public Optional<Long> getOptionalResourceHash() {

		if (optionalResourceHash == null) {

			return Optional.empty();
		}

		return optionalResourceHash;
	}

	public Optional<Map<String, Object>> getOptionalQualifiedAttributes() {

		if (optionalQualifiedAttributes == null) {

			return Optional.empty();
		}

		return optionalQualifiedAttributes;
	}

	public Statement build() {

		return new Statement(getOptionalSubjectNodeType(), getOptionalSubjectURI(), getOptionalSubjectId(), getOptionalSubjectDataModelURI(),
				getOptionalPredicateURI(), getOptionalObjectNodeType(), getOptionalObjectURI(), getOptionalObjectValue(), getOptionalObjectId(),
				getOptionalObjectDataModelURI(), getOptionalStatementUUID(), getOptionalResourceHash(), getOptionalQualifiedAttributes());
	}
}
