/*
 * Copyright (c) 2023 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.mappingio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MappingTreeView.ClassMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.FieldMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodArgMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodMappingView;
import net.fabricmc.mappingio.tree.MappingTreeView.MethodVarMappingView;

public class SubsetAssertingVisitor implements FlatMappingVisitor {
	public SubsetAssertingVisitor(MappingTreeView supTree, @Nullable MappingFormat supFormat, @Nullable MappingFormat subFormat) {
		this.supTree = supTree;
		this.supDstNsCount = supTree.getMaxNamespaceId();
		this.subHasNamespaces = subFormat == null ? true : subFormat.hasNamespaces;
		this.supHasNamespaces = supFormat == null ? true : supFormat.hasNamespaces;
		this.supHasFieldDesc = supFormat == null ? true : supFormat.hasFieldDescriptors;
		this.supHasArgs = supFormat == null ? true : supFormat.supportsArgs;
		this.supHasVars = supFormat == null ? true : supFormat.supportsLocals;
		this.supHasComments = supFormat == null ? true : supFormat.supportsComments;
	}

	@Override
	public void visitNamespaces(String srcNamespace, List<String> dstNamespaces) throws IOException {
		assertTrue(srcNamespace.equals(subHasNamespaces ? supTree.getSrcNamespace() : MappingUtil.NS_SOURCE_FALLBACK));
		this.dstNamespaces = dstNamespaces;

		if (!subHasNamespaces) {
			assertTrue(dstNamespaces.size() == 1);
			assertTrue(dstNamespaces.get(0).equals(MappingUtil.NS_TARGET_FALLBACK));
			return;
		}

		for (int i = 0; i < dstNamespaces.size(); i++) {
			String dstNs = dstNamespaces.get(i);
			boolean contained = supTree.getDstNamespaces().contains(dstNs);

			if (!supHasNamespaces) {
				if (contained) return;
			} else {
				assertTrue(contained);
			}
		}

		if (!supHasNamespaces) throw new RuntimeException("SubTree namespace not contained in SupTree");
	}

	@Override
	public boolean visitClass(String srcName, String[] dstNames) throws IOException {
		ClassMappingView supCls = supTree.getClass(srcName);
		Map<String, String> supDstNamesByNsName = new HashMap<>();

		if (supCls == null) {
			String[] tmpDst = supHasNamespaces ? dstNames : new String[]{dstNames[0]};
			if (!Arrays.stream(tmpDst).anyMatch(Objects::nonNull)) return true;
			throw new RuntimeException("SubTree class not contained in SupTree: " + srcName);
		}

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstNamesByNsName.put(supTree.getNamespaceName(supNs), supCls.getDstName(supNs));
		}

		for (int subNs = 0; subNs < dstNames.length; subNs++) {
			String supDstName = supDstNamesByNsName.get(dstNamespaces.get(subNs));
			if (!supHasNamespaces && supDstName == null) continue;
			assertTrue(dstNames[subNs] == null || dstNames[subNs].equals(supDstName) || (supDstName == null && dstNames[subNs].equals(srcName)));
		}

		return true;
	}

	@Override
	public void visitClassComment(String srcName, String[] dstNames, String comment) throws IOException {
		if (!supHasComments) return;
		assertEquals(supTree.getClass(srcName).getComment(), comment);
	}

	@Override
	public boolean visitField(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs) throws IOException {
		FieldMappingView supFld = supTree.getClass(srcClsName).getField(srcName, srcDesc);
		Map<String, String[]> supDstDataByNsName = new HashMap<>();

		if (supFld == null) {
			String[] tmpDst = supHasNamespaces ? dstNames : new String[]{dstNames[0]};
			if (!Arrays.stream(tmpDst).anyMatch(Objects::nonNull)) return true;
			throw new RuntimeException("SubTree field not contained in SupTree: " + srcName);
		}

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstDataByNsName.put(supTree.getNamespaceName(supNs), new String[]{supFld.getDstName(supNs), supFld.getDstDesc(supNs)});
		}

		for (int subNs = 0; subNs < dstNames.length; subNs++) {
			String[] supDstData = supDstDataByNsName.get(dstNamespaces.get(subNs));
			if (!supHasNamespaces && supDstData == null) continue;

			String supDstName = supDstData[0];
			assertTrue(dstNames[subNs] == null || dstNames[subNs].equals(supDstName) || (supDstName == null && dstNames[subNs].equals(srcName)));

			if (!supHasFieldDesc) continue;
			String supDstDesc = supDstData[1];
			assertTrue(dstDescs == null || dstDescs[subNs] == null || dstDescs[subNs].equals(supDstDesc));
		}

		return true;
	}

	@Override
	public void visitFieldComment(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs, String comment) throws IOException {
		if (!supHasComments) return;
		assertEquals(supTree.getClass(srcClsName).getField(srcName, srcDesc).getComment(), comment);
	}

	@Override
	public boolean visitMethod(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs) throws IOException {
		MethodMappingView supMth = supTree.getClass(srcClsName).getMethod(srcName, srcDesc);
		Map<String, String[]> supDstDataByNsName = new HashMap<>();

		if (supMth == null) {
			String[] tmpDst = supHasNamespaces ? dstNames : new String[]{dstNames[0]};
			if (!Arrays.stream(tmpDst).anyMatch(Objects::nonNull)) return true;
			throw new RuntimeException("SubTree method not contained in SupTree: " + srcName);
		}

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstDataByNsName.put(supTree.getNamespaceName(supNs), new String[]{supMth.getDstName(supNs), supMth.getDstDesc(supNs)});
		}

		for (int subNs = 0; subNs < dstNames.length; subNs++) {
			String[] supDstData = supDstDataByNsName.get(dstNamespaces.get(subNs));
			if (!supHasNamespaces && supDstData == null) continue;

			String supDstName = supDstData[0];
			assertTrue(dstNames[subNs] == null || dstNames[subNs].equals(supDstName) || (supDstName == null && dstNames[subNs].equals(srcName)));

			String supDstDesc = supDstData[1];
			assertTrue(dstDescs == null || dstDescs[subNs] == null || dstDescs[subNs].equals(supDstDesc));
		}

		return true;
	}

	@Override
	public void visitMethodComment(String srcClsName, String srcName, String srcDesc,
			String[] dstClsNames, String[] dstNames, String[] dstDescs, String comment) throws IOException {
		if (!supHasComments) return;
		assertEquals(supTree.getClass(srcClsName).getMethod(srcName, srcDesc).getComment(), comment);
	}

	@Override
	public boolean visitMethodArg(String srcClsName, String srcMethodName, String srcMethodDesc, int argPosition, int lvIndex, String srcName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstNames) throws IOException {
		if (!supHasArgs) return false;
		MethodArgMappingView supArg = supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getArg(argPosition, lvIndex, srcName);
		Map<String, String> supDstNamesByNsName = new HashMap<>();

		if (supArg == null) {
			String[] tmpDst = supHasNamespaces ? dstNames : new String[]{dstNames[0]};
			if (!Arrays.stream(tmpDst).anyMatch(Objects::nonNull)) return true;
			throw new RuntimeException("SubTree arg not contained in SupTree: " + srcName);
		}

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstNamesByNsName.put(supTree.getNamespaceName(supNs), supArg.getDstName(supNs));
		}

		for (int subNs = 0; subNs < dstNames.length; subNs++) {
			String supDstName = supDstNamesByNsName.get(dstNamespaces.get(subNs));
			if (!supHasNamespaces && supDstName == null) continue;
			assertTrue(dstNames[subNs] == null || dstNames[subNs].equals(supDstName) || (supDstName == null && dstNames[subNs].equals(srcName)));
		}

		return true;
	}

	@Override
	public void visitMethodArgComment(String srcClsName, String srcMethodName, String srcMethodDesc, int argPosition, int lvIndex, String srcArgName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstNames, String comment) throws IOException {
		if (!supHasComments) return;
		assertEquals(supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getArg(argPosition, lvIndex, srcArgName).getComment(), comment);
	}

	@Override
	public boolean visitMethodVar(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstNames) throws IOException {
		if (!supHasVars) return false;
		MethodVarMappingView supVar = supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcName);
		Map<String, String> supDstNamesByNsName = new HashMap<>();

		if (supVar == null) {
			String[] tmpDst = supHasNamespaces ? dstNames : new String[]{dstNames[0]};
			if (!Arrays.stream(tmpDst).anyMatch(Objects::nonNull)) return true;
			throw new RuntimeException("SubTree var not contained in SupTree: " + srcName);
		}

		for (int supNs = 0; supNs < supDstNsCount; supNs++) {
			supDstNamesByNsName.put(supTree.getNamespaceName(supNs), supVar.getDstName(supNs));
		}

		for (int subNs = 0; subNs < dstNames.length; subNs++) {
			String supDstName = supDstNamesByNsName.get(dstNamespaces.get(subNs));
			if (!supHasNamespaces && supDstName == null) continue;
			assertTrue(dstNames[subNs] == null || dstNames[subNs].equals(supDstName) || (supDstName == null && dstNames[subNs].equals(srcName)));
		}

		return true;
	}

	@Override
	public void visitMethodVarComment(String srcClsName, String srcMethodName, String srcMethodDesc,
			int lvtRowIndex, int lvIndex, int startOpIdx, int endOpIdx, String srcVarName,
			String[] dstClsNames, String[] dstMethodNames, String[] dstMethodDescs, String[] dstNames, String comment) throws IOException {
		if (!supHasComments) return;
		assertEquals(supTree.getClass(srcClsName).getMethod(srcMethodName, srcMethodDesc).getVar(lvtRowIndex, lvIndex, startOpIdx, endOpIdx, srcVarName).getComment(), comment);
	}

	private final MappingTreeView supTree;
	private final int supDstNsCount;
	private final boolean subHasNamespaces;
	private final boolean supHasNamespaces;
	private final boolean supHasFieldDesc;
	private final boolean supHasArgs;
	private final boolean supHasVars;
	private final boolean supHasComments;
	private List<String> dstNamespaces;
}

