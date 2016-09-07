package de.bb.bejy.j2ee;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;

class TS implements TimerService {

	public Timer createTimer(long arg0, Serializable arg1)
			throws IllegalArgumentException, IllegalStateException, EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public Timer createTimer(Date arg0, Serializable arg1)
			throws IllegalArgumentException, IllegalStateException, EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public Timer createTimer(long arg0, long arg1, Serializable arg2)
			throws IllegalArgumentException, IllegalStateException, EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public Timer createTimer(Date arg0, long arg1, Serializable arg2)
			throws IllegalArgumentException, IllegalStateException, EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection getTimers() throws IllegalStateException, EJBException {
		return Collections.emptyList();
	}

	public Timer createCalendarTimer(ScheduleExpression arg0)
			throws IllegalArgumentException, IllegalStateException, EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public Timer createCalendarTimer(ScheduleExpression arg0, TimerConfig arg1)
			throws IllegalArgumentException, IllegalStateException, EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public Timer createIntervalTimer(long arg0, long arg1, TimerConfig arg2)
			throws IllegalArgumentException, IllegalStateException, EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public Timer createIntervalTimer(Date arg0, long arg1, TimerConfig arg2)
			throws IllegalArgumentException, IllegalStateException, EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public Timer createSingleActionTimer(long arg0, TimerConfig arg1)
			throws IllegalArgumentException, IllegalStateException, EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public Timer createSingleActionTimer(Date arg0, TimerConfig arg1)
			throws IllegalArgumentException, IllegalStateException, EJBException {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<Timer> getAllTimers() throws IllegalStateException, EJBException {
		// TODO Auto-generated method stub
		return null;
	}

}
