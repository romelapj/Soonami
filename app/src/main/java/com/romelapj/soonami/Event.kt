package com.romelapj.soonami

/**
 * {@Event} represents an earthquake event. It holds the details
 * of that event such as title (which contains magnitude and location
 * of the earthquake), as well as time, and whether or not a tsunami
 * alert was issued during the earthquake.
 */
class Event
/**
 * Constructs a new [Event].
 *
 * @param eventTitle is the title of the earthquake event
 * @param eventTime is the time the earhtquake happened
 * @param eventTsunamiAlert is whether or not a tsunami alert was issued
 */
(
        /** Title of the earthquake event  */
        val title: String,
        /** Time that the earthquake happened (in milliseconds)  */
        val time: Long,
        /** Whether or not a tsunami alert was issued (1 if it was issued, 0 if no alert was issued)  */
        val tsunamiAlert: Int)