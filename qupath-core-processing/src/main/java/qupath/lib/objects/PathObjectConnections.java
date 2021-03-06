/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package qupath.lib.objects;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data structure for storing multiple PathObjectConnectionGroups.
 * 
 * @author Pete Bankhead
 *
 */
public class PathObjectConnections implements Externalizable {
	
	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = LoggerFactory.getLogger(PathObjectConnections.class);
	
	private List<PathObjectConnectionGroup> connections = new ArrayList<>();
	
	public PathObjectConnections() {}
	
	public List<PathObjectConnectionGroup> getConnectionGroups() {
		return Collections.unmodifiableList(connections);
	}
	
	public void addGroup(final PathObjectConnectionGroup group) {
		connections.add(group);
	}

	public boolean removeGroup(final PathObjectConnectionGroup group) {
		return connections.remove(group);
	}
	
	public boolean isEmpty() {
		return connections.isEmpty();
	}
	
	public void clear() {
		connections.clear();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(1);
		out.writeObject(connections);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		long version = in.readLong();
		if (version != 1) {
			logger.warn("Unexpected {} version number {}", PathObjectConnections.class, version);
		}
		List<PathObjectConnectionGroup> list = (List<PathObjectConnectionGroup>)in.readObject();
		connections.addAll(list);
	}

}
