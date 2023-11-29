package aifolk.core;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import aifolk.core.MLManagementShard.DataContext;

public class DrivingDataContext extends DataContext {
	List<Long>	pedestrianNumber	= new LinkedList<>();
	int			windowSize			= 5;
	
	public void addPedestrianNumberData(Long number) {
		pedestrianNumber.add(number);
		if(pedestrianNumber.size() > windowSize)
			pedestrianNumber.remove(0);
	}
	
	public Long getMaxPedestrians() {
		return Collections.max(pedestrianNumber);
	}
}
