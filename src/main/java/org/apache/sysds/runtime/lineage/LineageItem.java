/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sysds.runtime.lineage;

import org.apache.sysds.runtime.DMLRuntimeException;
import org.apache.sysds.runtime.controlprogram.parfor.util.IDSequence;
import org.apache.sysds.runtime.util.UtilFunctions;

public class LineageItem {
	private static IDSequence _idSeq = new IDSequence();
	
	private final long _id;
	private final String _opcode;
	private final String _data;
	private final LineageItem[] _inputs;
	private int _hash = 0;
	// init visited to true to ensure visited items are
	// not hidden when used as inputs to new items
	private boolean _visited = true;
	
	public enum LineageItemType {Literal, Creation, Instruction, Dedup}
	public static final String dedupItemOpcode = "dedup";
	
	public LineageItem() {
		this("");
	}
	
	public LineageItem(String data) {
		this(_idSeq.getNextID(), data);
	}

	public LineageItem(long id, String data) {
		this(id, data, "", null);
	}
	
	public LineageItem(String data, String opcode) {
		this(_idSeq.getNextID(), data, opcode, null);
	}
	
	public LineageItem(String opcode, LineageItem[] inputs) { 
		this(_idSeq.getNextID(), "", opcode, inputs);
	}

	public LineageItem(String data, String opcode, LineageItem[] inputs) {
		this(_idSeq.getNextID(), data, opcode, inputs);
	}
	
	public LineageItem(LineageItem li) {
		this(_idSeq.getNextID(), li);
	}
	
	public LineageItem(long id, LineageItem li) {
		this(id, li._data, li._opcode, li._inputs);
	}
	
	public LineageItem(long id, String data, String opcode, LineageItem[] inputs) {
		_id = id;
		_opcode = opcode;
		_data = data;
		_inputs = inputs;
	}
	
	public LineageItem[] getInputs() {
		return _inputs;
	}
	
	public String getData() {
		return _data;
	}
	
	public boolean isVisited() {
		return _visited;
	}
	
	public void setVisited() {
		setVisited(true);
	}
	
	public void setVisited(boolean flag) {
		_visited = flag;
	}
	
	public long getId() {
		return _id;
	}
	
	public String getOpcode() {
		return _opcode;
	}
	
	public LineageItemType getType() {
		if (_opcode.equals(dedupItemOpcode))
			return LineageItemType.Dedup;
		if (isLeaf() && isInstruction())
			return LineageItemType.Creation;
		else if (isLeaf() && !isInstruction())
			return LineageItemType.Literal;
		else if (!isLeaf() && isInstruction())
			return LineageItemType.Instruction;
		else
			throw new DMLRuntimeException("An inner node could not be a literal!");
	}
	
	@Override
	public String toString() {
		return LineageItemUtils.explainSingleLineageItem(this);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LineageItem))
			return false;
		
		resetVisitStatus();
		boolean ret = equalsLI((LineageItem) o);
		resetVisitStatus();
		return ret;
	}
	
	private boolean equalsLI(LineageItem that) {
		if (isVisited() || this == that)
			return true;
		
		boolean ret = _opcode.equals(that._opcode);
		ret &= _data.equals(that._data);
		
		if (_inputs != null && ret && (_inputs.length == that._inputs.length))
			for (int i = 0; i < _inputs.length; i++)
				ret &= _inputs[i].equalsLI(that._inputs[i]);
		
		setVisited();
		return ret;
	}
	
	@Override
	public int hashCode() {
		if (_hash == 0) {
			//compute hash over opcode and all inputs
			int h = _opcode.hashCode();
			if (_inputs != null)
				for (LineageItem li : _inputs)
					h = UtilFunctions.intHashCode(h, li.hashCode());
			_hash = UtilFunctions.intHashCode(h, _data.hashCode());
		}
		return _hash;
	}

	public LineageItem deepCopy() { //bottom-up
		if (isLeaf())
			return new LineageItem(this);
		
		LineageItem[] copyInputs = new LineageItem[getInputs().length];
		for (int i=0; i<_inputs.length; i++) 
			copyInputs[i] = _inputs[i].deepCopy();
		return new LineageItem(_opcode, copyInputs);
	}
	
	public boolean isLeaf() {
		return _inputs == null || _inputs.length == 0;
	}
	
	public boolean isInstruction() {
		return !_opcode.isEmpty();
	}
	
	public LineageItem resetVisitStatus() {
		if (!isVisited())
			return this;
		if (_inputs != null)
			for (LineageItem li : getInputs())
				li.resetVisitStatus();
		setVisited(false);
		return this;
	}
	
	public static void resetVisitStatus(LineageItem[] lis) {
		if (lis != null)
			for (LineageItem liRoot : lis)
				liRoot.resetVisitStatus();
	}
	
	public static void resetIDSequence() {
		_idSeq.reset(-1);
	}
}
