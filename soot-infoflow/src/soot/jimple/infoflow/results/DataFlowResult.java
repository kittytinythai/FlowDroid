package soot.jimple.infoflow.results;

import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ConditionExpr;
import soot.jimple.IfStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkCategory;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

import java.util.HashSet;
import java.util.Set;

/**
 * Data class for having the source and sink of a single flow together in one
 * place
 * 
 * @author Steven Arzt
 *
 */
public class DataFlowResult {

	private final ResultSourceInfo source;
	private final ResultSinkInfo sink;
	private int pathLength;
	private int numClasses;
	private int numMethods;
	private float implicitFactor;
	private float priorityScore;

	public DataFlowResult(ResultSourceInfo source, ResultSinkInfo sink) {
		this.source = source;
		this.sink = sink;
		// if a dataflow result doesn't have a score, then it should not get the best score
		this.priorityScore = Integer.MAX_VALUE;
	}

	public DataFlowResult(ResultSourceInfo source, ResultSinkInfo sink, IInfoflowCFG iCfg) {
		this.source = source;
		this.sink = sink;
		computeDataFlowInfo(iCfg);
	}

	public ResultSourceInfo getSource() {
		return source;
	}

	public ResultSinkInfo getSink() {
		return sink;
	}

	public float getPriorityScore() {
		return priorityScore;
	}

	public int getPathLength() {
		return pathLength;
	}

	public int getNumClasses() {
		return numClasses;
	}

	public int getNumMethods() {
		return numMethods;
	}

	public float getImplicitFactor() {
		return implicitFactor;
	}

	// Returns the total number of conditional expressions in a conditional statement
	private int countConditionExpr(Value value) {
		if (value instanceof ConditionExpr) {
			Value op1 = ((ConditionExpr) value).getOp1();
			Value op2 = ((ConditionExpr) value).getOp2();
			return 1 + countConditionExpr(op1) + countConditionExpr(op2);
		} else {
			return 0;
		}
	}

	// Compute the following fields: pathLength, numClasses, numMethods, implicitFactor and priorityScore
	private void computeDataFlowInfo(IInfoflowCFG iCfg) {
		Set<String> methods = new HashSet<>();
		Set<String> classes = new HashSet<>();

		for (Unit p : source.getPath()) {
			SootMethod method = iCfg.getMethodOf(p);
			methods.add(method.getName());
			classes.add(method.getDeclaringClass().getName());
			if (p instanceof IfStmt) {
				Value condition = ((IfStmt) p).getCondition();
				int numConds = countConditionExpr(condition);
				implicitFactor += (float) 1 / numConds;
			} else if (p instanceof LookupSwitchStmt || p instanceof TableSwitchStmt) {
				implicitFactor++;
			}
		}

		pathLength = source.getPathLength();
		numMethods = methods.size();
		numClasses = classes.size();

		// include conditionals
		priorityScore = pathLength + numMethods + numClasses + implicitFactor;

		// include conditionals and normalize based on path length
		//priorityScore = (numMethods + numClasses + implicitFactor) / pathLength;
	}

	/**
	 * Convenience function to get the source category ID
	 * 
	 * @return The source category ID or <code>null</code> if no source data is
	 *         available
	 */
	public String getSourceCategoryID() {
		ISourceSinkDefinition sourceDef = source.getDefinition();
		if (sourceDef != null) {
			ISourceSinkCategory sourceCat = sourceDef.getCategory();
			if (sourceCat != null)
				return sourceCat.getID();
		}
		return null;
	}

	/**
	 * Convenience function to get the sink category ID
	 * 
	 * @return The sink category ID or <code>null</code> if no sink data is
	 *         available
	 */
	public String getSinkCategoryID() {
		ISourceSinkDefinition sinkDef = sink.getDefinition();
		if (sinkDef != null) {
			ISourceSinkCategory sinkCat = sinkDef.getCategory();
			if (sinkCat != null)
				return sinkCat.getID();
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(source.toString());
		sb.append(" -> ");
		sb.append(sink.toString());
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sink == null) ? 0 : sink.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataFlowResult other = (DataFlowResult) obj;
		if (sink == null) {
			if (other.sink != null)
				return false;
		} else if (!sink.equals(other.sink))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

}
