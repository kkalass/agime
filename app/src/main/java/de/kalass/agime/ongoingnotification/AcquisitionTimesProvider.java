package de.kalass.agime.ongoingnotification;

import de.kalass.agime.acquisitiontime.AcquisitionTimes;

/**
 * Interface for providing AcquisitionTimes data. This abstraction allows for 
 * dependency injection and easier testing by decoupling the NotificationWorker
 * from direct database access.
 */
public interface AcquisitionTimesProvider {
    
    /**
     * Retrieves the current acquisition times based on the current date and time.
     * 
     * @return AcquisitionTimes containing current, previous and next acquisition time instances
     */
    AcquisitionTimes getCurrentAcquisitionTimes();
}
