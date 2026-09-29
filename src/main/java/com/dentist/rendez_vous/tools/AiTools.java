package com.dentist.rendez_vous.tools;

import com.dentist.rendez_vous.dto.CreneauDisponibleDto;
import com.dentist.rendez_vous.dto.EventsResponse;
import com.dentist.rendez_vous.service.GoogleCalendarService;
import com.dentist.rendez_vous.service.RdvService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor

public class AiTools {
    private final GoogleCalendarService calendarService;
    private final RdvService rdvService;

    @Tool(name = "getCreneauxLibres",
            description = """
                 Retrieves the list of available time slots for a specific date. Use this tool when the\s
                 clinic manager asks about availability, free time slots, or wants to know which time slots\s
                 are still available on a given date (today, tomorrow, a specific date, or a relative date such as "next\s
                 week") — for example, to schedule a new patient or check the workload for a particular day.\s
                 Returns a list of time slots with their start and end times. If no time slot is available, the\s
                 returned list will be empty.
            """)
    public List<CreneauDisponibleDto> getDisponibleCrenau(@ToolParam(description = """
            The date for which to search for available time slots, in ISO format (YYYY-MM-DD).\s
            Always convert the natural language expression used by the clinic manager into an absolute date\s
            before calling the tool, using today's date as the reference. Conversion examples:\s
            "today" → today's date;\s
            "tomorrow" or "the next day" → today's date + 1 day;\s
            "the day after tomorrow" → today's date + 2 days;\s
            "next week" → the Monday of the following week (or ask for clarification if the exact day\s
            is not specified);\s
            "next Monday", "next Friday", etc. → calculate the next occurrence of that day of the week\s
            starting from today;\s
            an explicit date ("August 20", "09/15/2026") → convert directly to ISO format.\s
            Never return a date that has already passed relative to today.
            """) LocalDate day){
        return calendarService.getCreneauxLibres(day);
    }

    @Tool(name = "getDayEvents",
            description = """
        Retrieves the complete list of appointments already scheduled at the clinic for a specific date. Use\s
        this tool when the clinic manager asks to view the agenda, the day's schedule, the list of\s
        expected patients, or who has an appointment on a given date (today, tomorrow, a specific date, or a\s
        relative date such as "next week"). Returns for each appointment the start and end times,\s
        the reason, the description, as well as the name and email of the associated patient — particularly useful\s
        for finding an event ID before canceling or modifying it.
        
        Important: some calendar events may have been created directly by the clinic in\s
        Google Calendar without going through the application (for example, time blocks, personal appointments,\s
        or administrative tasks). For these events, the email and name fields will be empty\s
        or absent — this is not an error; it simply means that no patient from the application is\s
        associated with the event. In this case, present the event with its title, description, and schedule,\s
        without inventing a name or email, and clearly indicate to the manager that it is not a\s
        patient appointment registered in the system.
        
        If no appointments exist for that date, the returned list will be empty.
        """)
    public List<EventsResponse> getDayEvents(@ToolParam(description = """
        The date for which to retrieve scheduled appointments, in ISO format (YYYY-MM-DD).\s
        Always convert the natural language expression used by the clinic manager into an absolute date\s
        before calling the tool, using today's date as the reference. Conversion examples:\s
        "today" → today's date;\s
        "tomorrow" or "the next day" → today's date + 1 day;\s
        "the day after tomorrow" → today's date + 2 days;\s
        "next week" → the Monday of the following week (or ask for clarification if the exact day\s
        is not specified);\s
        "next Monday", "next Friday", etc. → calculate the next occurrence of that day of the week\s
        starting from today;\s
        an explicit date ("August 20", "09/15/2026") → convert directly to ISO format.
        """) LocalDate day){
        return rdvService.getEvenementsDuJourForAiAgent(day);
    }

    @Tool(name = "cancelAppointment",
            description = """
        Cancels an existing appointment in the clinic's calendar using its Google Calendar ID.
        Use this tool when the clinic manager explicitly asks to cancel, delete, or remove
        a specific appointment. Before calling this tool, always retrieve the ID of the
        relevant event using the agenda consultation tool (getDayEvents), based on the
        context provided by the manager (patient name, time, reason, etc.) to identify the correct event.
        
        If the appointment is associated with a patient registered in the application, the patient will
        automatically receive an email informing them of the cancellation and its reason. If the event was
        created directly in Google Calendar by the clinic (without an associated patient), it will simply
        be deleted from the calendar without sending an email.
        
        Once the action is completed, clearly confirm to the manager that the appointment has been cancelled.
        """)
    public void cancelAppointment(@ToolParam(description = """
        The Google Calendar ID (googleEventId) of the appointment to cancel. Always retrieve this
        ID beforehand using the getDayEvents tool based on the criteria provided by the
        clinic manager (date, time, patient name, reason, etc.) present in the conversation —
        never invent or assume an ID.
        """) String googleEventId,
                                  @ToolParam(description = """
        The reason for the cancellation, formulated as a clear and complete sentence, intended to be read
        directly by the patient in the notification email. Infer this reason from the context provided
        by the clinic manager in the conversation (for example: practitioner unavailability, an unexpected
        issue, a patient request, etc.). If no explicit reason is provided by the manager, formulate a
        neutral and professional reason (for example: "due to an unexpected issue at the clinic").
        """) String raison){
        rdvService.deleteEvent(googleEventId, raison);
    }

    @Tool(
            name = "createEvent",
            description = """
                Creates an event in the clinic's calendar at the manager's request — whether to block
                a period of unavailability (unexpected issue, leave, meeting, break) or for any other
                custom task they want to add to the calendar. This tool does NOT create a patient appointment
                (use another tool for that); it is only used to occupy a time slot in the calendar.
                
                The manager may express their request in relative or approximate terms (for example:
                "block the entire morning tomorrow", "block this afternoon", "reserve 1:00 PM to 2:30 PM
                on Wednesday") — always determine the exact parameters (date, start time, duration) from
                the conversation context before calling this tool, using today's date as the reference for
                any relative expression.
                
                This tool automatically applies the clinic's rules and may reject creation in the following cases,
                which you must clearly explain to the manager:
                - Time outside the clinic's opening/closing hours.
                - Date or time already in the past relative to now.
                - Time slot already occupied by an existing event (patient appointment or another task) — in this
                  case, inform the manager that an event already occupies this time slot and suggest consulting
                  the day's agenda (using the consultation tool) or choosing another time.
                """
    )
    public void createEvent(
            @ToolParam(description = """
                The event title. If the manager does not specify an explicit title, infer a short
                and clear one from the context (for example "Unavailable", "Meeting", "Lunch Break",
                "Leave"). Never leave this field empty or use a generic title unrelated to the request.
                """) String titre,
            @ToolParam(description = """
                The event description. If the manager does not provide one, generate a short
                and neutral description consistent with the title (for example "Blocked time slot, clinic unavailable"
                for an unavailability period, or a simple reformulation of the initial request).
                """) String description,
            @ToolParam(description = """
                The event date, in ISO format (YYYY-MM-DD). The manager must specify it explicitly
                or express it in natural language (today, tomorrow, next Wednesday, etc.) — always convert
                this expression into an absolute date before calling the tool, using today's date as the
                reference. Never call the tool without determining a precise date.
                """) LocalDate date,
            @ToolParam(description = """
                The event start time, in HH:mm format (24-hour). If the manager provides an explicit time
                (e.g. "13:00"), use it directly. If the expression is approximate or refers to a named period,
                infer a start time consistent with the clinic's opening hours, for example: "morning" →
                clinic opening time; "afternoon" → beginning of the afternoon (e.g. 14:00); "this week" or
                any expression without a precise time → ask the manager for clarification rather than
                guessing an arbitrary time.
                """) LocalTime heure,
            @ToolParam(description = """
                The event duration in minutes. If the manager provides an explicit duration (e.g. "60 min",
                "1h30"), convert it into minutes. If the expression refers to a named period without a
                numerical duration (for example "the entire morning" or "the entire afternoon"), calculate
                the duration in minutes corresponding to that period within the clinic's opening hours
                (from the beginning of the period until the lunch break, or from the afternoon reopening
                until closing). Never invent an arbitrary duration without a basis in the context or
                the clinic's opening hours.
                """) int duree
    ){
        rdvService.createEventByAiAgent(titre, description, date, heure, duree);
    }

    @Tool(
            name = "getEventsInIntervalDate",
            description = """
                Retrieves the list of all events in the clinic's calendar over a given period (between
                a start date and an end date). Use this tool when the clinic manager asks to view the agenda
                over several days, a week, or a specific date range — for example:
                "show me my appointments for the next 3 days", "what do I have this week",
                "the agenda from September 10 to 15", or "the appointments for the next 5 days".
                
                Returns for each event the start and end times, the reason, the description, as well as
                the name and email of the associated patient if a patient from the application is linked
                to the event. Some events may have been created directly in Google Calendar by the clinic
                (unavailability periods, personal tasks, meetings) without being linked to a patient —
                in this case, the email and name fields will be empty; this is not an error. Simply
                present the event with its title, description, and schedule, clearly indicating to the
                manager that it is not a patient appointment registered in the system.
                
                If no event exists during the requested period, the returned list will be empty — simply
                inform the manager without presenting it as an error.
                """
    )
    public List<EventsResponse> getEventByIntervalDate(
            @ToolParam(description = """
                The start date of the search period, in ISO format (YYYY-MM-DD). Always convert
                the manager's natural language expression into an absolute date before calling the tool,
                using today's date as the reference. Examples: "this week" → the current day or the
                Monday of the current week depending on the context; "the next 3 days" or "the next 5
                days" → today's date; "next week" → the Monday of the following week; an explicit date
                range ("from September 10 to 15") → convert directly to ISO format.
                """) LocalDate debut,
            @ToolParam(description = """
                The end date of the search period, in ISO format (YYYY-MM-DD), always greater than or
                equal to the start date. Determine it from the context of the request: "the next 3 days" →
                start date + 2 days; "the next 5 days" → start date + 4 days; "this week" → the Sunday
                of the current week; "next week" → the Sunday of the following week; an explicit date
                ("until September 15") → convert directly to ISO format. If the requested period is
                ambiguous (for example "soon" without further details), ask the manager for clarification
                rather than guessing an arbitrary end date.
                """) LocalDate fin
    ){
        return rdvService.getEventsByIntervalForAiAgent(debut, fin);
    }

    @Tool(
            name = "getEventByRaison",
            description = """
                Searches the clinic's calendar for all events whose title, description, or reason
                contains a specific keyword provided by the manager. Use this tool when the manager
                asks to find appointments by theme or type, for example "show me all radiology
                appointments", "how many consultations have I had", "find the dental cleaning appointments",
                or "do you have any appointments concerning [patient name or reason]".
                
                This is a broad text-based search (it may find the keyword in the title, description, or
                other event fields) and is not limited to a specific period — it searches the entire calendar.
                If the result is too broad or not relevant enough, suggest that the manager specify a period
                using the date range search tool as a complement.
                
                Returns for each event the start and end times, the reason, the description,
                as well as the name and email of the associated patient if a patient from the application
                is linked to the event. Some events may have been created directly in Google Calendar
                by the clinic without being linked to a patient — in this case, the email and name fields
                will be empty; this is not an error, simply present the event with the available information.
                
                If no event matches the searched keyword, the returned list will be empty — inform the
                manager without presenting it as an error, and optionally suggest reformulating the keyword.
                """
    )
    public List<EventsResponse> getEventByRaison(
            @ToolParam(description = """
                The keyword or expression to search for among calendar events (for example
                "radiology", "consultation", "dental cleaning", or a patient's name). Extract this term
                from the context of the manager's request, keeping it simple and direct — do not include
                a complete sentence or unnecessary words around the search term itself.
                """) String raison
    ){
        return rdvService.getEventsByRaisonForAiAgent(raison);
    }

    @Tool(name = "marquerRendezVousTermine",
            description = """
        Marks an appointment as completed (status "passed") after the clinic manager has
        actually received and treated the patient concerned. Use this tool only when the
        manager explicitly indicates that an appointment has just ended or has been completed
        (for example: "mark the 10:00 appointment as completed", "the patient just left,
        close their appointment").
        
        Before calling this tool, always retrieve the ID of the relevant event using the
        agenda consultation tool (getDayEvents or equivalent), based on the context provided
        by the manager (patient name, time, reason, etc.) to identify the correct event.
        
        This tool only works for events corresponding to a patient appointment registered
        in the application. If the provided ID corresponds to an event created directly in Google
        Calendar by the clinic (without an associated patient, such as an unavailability period or task),
        the tool will return a message clearly indicating this — in that case, inform the manager that
        this event is not a patient appointment and therefore cannot be marked as "completed" in this way.
        
        Upon success, clearly confirm to the manager that the appointment has been completed.
        """)
    public String marquerRendezVousTermine(@ToolParam(description = """
        The Google Calendar ID (googleEventId) of the appointment to mark as completed. Always retrieve
        this ID beforehand using the agenda consultation tool, based on the criteria provided by the
        manager (date, time, patient name, reason, etc.) present in the conversation — never invent
        or assume an ID.
        """) String googleEventId){
        return rdvService.eventPasse(googleEventId);
    }
}