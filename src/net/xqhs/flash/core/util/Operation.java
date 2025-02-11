package net.xqhs.flash.core.util;

import java.util.Arrays;

import net.xqhs.flash.core.agent.AgentWave;

/**
 * An operation is something that an entity offers and which has a name and some arguments and can be accessed via agent
 * waves. Normally the name of the operation is the last in the endpoint which is the destination of the wave.
 * <p>
 * To make it easier to have constants for operation names, we pair each instance of {@link Operation} with an instance
 * of {@link OperationName}, for which the {@link Object#toString()} method is used to obtain the actual name. Since
 * entities are likely to have multiple operations, it is handy to store the names of the operations in an {@link Enum}
 * implementing {@link OperationName}.
 * <p>
 * For other constants, such as names for the parameters, or even fixed values for the parameters, one can use the
 * {@link Field} interface, similarly as implemented by an {@link Enum}.
 * <p>
 * An operation may have a number of fixed parameters, oh which the names are known beforehand, as well as a number of
 * "free" parameters.
 */
public interface Operation {
	/**
	 * Marker interface for classes (usually {@link Enum}s) whose instances are operation names.
	 * 
	 * @see Operation
	 */
	public interface OperationName {
		// marker interface
	}
	
	/**
	 * Marker interface for classes (usually {@link Enum}s) whose instances are field (or even value) names.
	 * 
	 * @see Operation
	 */
	public interface Field {
		// marker interface
	}
	
	AgentWave instantiate(String destinationEntity, Object... args);
	
	AgentWave instantiate(String destinationEntity, String varParNames[], Object... args);
	
	/**
	 * @return the name of the operation.
	 */
	String getOperation();
	
	/**
	 * @return the name of the operation (same as {@link #getOperation()}, but shorter).
	 */
	default String s() {
		return getOperation();
	}
	
	/**
	 * A method allowing to obtain the {@link Operation} instance corresponding to a specific name, in a concrete
	 * {@link Operation} - {@link OperationName} pairing. If {@link OperationName} is implemented by an {@link Enum},
	 * this can be done by the enum finding the appropriate constant and then finding the corresponding
	 * {@link Operation} implementation.
	 * 
	 * TODO it is unclear how this can actually be done in practice.
	 * 
	 * @param s
	 *            - the name of the operation, as a {@link String}.
	 * @return the corresponding {@link Operation}.
	 */
	Operation fromString(String s);
	
	// static
	
	public class BaseOperation implements Operation {
		String		name;
		String[] parameters;
		
		public BaseOperation(OperationName operationName, String... parameterNames) {
			name = operationName.toString();
			parameters = parameterNames != null ? parameterNames : new String[] {};
		}
		
		public BaseOperation(OperationName operationName, Field... parameterNames) {
			name = operationName.toString();
			parameters = parameterNames != null
					? Arrays.asList(parameterNames).stream().map(p -> p.toString()).toArray(String[]::new)
					: new String[] {};
		}

		@Override
		public AgentWave instantiate(String destinationEntity, String varParNames[], Object... args) {
			AgentWave result = new AgentWave().appendDestination(destinationEntity, name.toLowerCase());
			for(int i = 0; i < Math.min(parameters.length, args.length); i++)
				if(args[i] instanceof String)
					result.add(parameters[i], (String) args[i]);
				else
					result.addObject(parameters[i], args[i]);
			if(varParNames != null)
				for(int i = 0; i < Math.min(varParNames.length, args.length - parameters.length); i++)
					if(args[i + parameters.length] instanceof String)
						result.add(varParNames[i], (String) args[i + parameters.length]);
					else
						result.addObject(varParNames[i], args[i + parameters.length]);
			return result;
		}
		
		@Override
		public AgentWave instantiate(String destinationEntity, Object... args) {
			return instantiate(destinationEntity, null, args);
		}
		
		@Override
		public String getOperation() {
			return name;
		}
		
		@Override
		public Operation fromString(String s) {
			// TODO Auto-generated method stub
			return null;
		}
	}
}
