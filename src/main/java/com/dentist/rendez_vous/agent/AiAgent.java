package com.dentist.rendez_vous.agent;

import com.dentist.rendez_vous.tools.AiTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class AiAgent {
    private final ChatClient chatClient;

    @Value("${app.begin.hour}")
    private int heureOuverture;
    @Value("${app.begin.minute}")
    private int minuteOuverture;
    @Value("${app.end.hour}")
    private int heureFermeture;
    @Value("${app.end.minute}")
    private int minuteFermeture;

    public AiAgent(ChatClient.Builder builder, ChatMemory chatMemory, AiTools aiTools) {

        this.chatClient = builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(aiTools)
                .build();
    }

    public Flux<String> askAgent(
            String query,
            String convId
    ) {

        LocalDateTime maintenant = LocalDateTime.now();
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
        DateTimeFormatter formatterHeure = DateTimeFormatter.ofPattern("HH:mm");

        LocalTime ouverture = LocalTime.of(heureOuverture, minuteOuverture);
        LocalTime fermeture = LocalTime.of(heureFermeture, minuteFermeture);

        String systemPrompt = """
                You are the internal assistant of the dental practice, intended exclusively for the practice manager to help them manage the schedule: check available time slots, view appointments scheduled for a specific date or period, search for events by subject, create events (unavailability periods, tasks), and cancel or modify existing appointments, according to the available tools.
                TIME CONTEXT
                
                We are currently on %s, and the current time is %s.
                You MUST use this date and time as the reference for any conversion of relative expressions (today, tomorrow, next week, this afternoon, etc.), and never use any other date or time you may know from elsewhere. Always convert these expressions into absolute date/time values in ISO format before calling a tool.
                
                PRACTICE OPENING HOURS
                
                The practice is open from %s to %s, Monday through Friday (unless otherwise specified). Keep these hours in mind when interpreting the manager's requests (for example, "the morning" refers to the period between opening time and the lunch break, while "the afternoon" refers to the period between reopening and closing), and anticipate that any creation or modification outside these hours will be rejected by the tools.
                
                COMBINING TOOLS FOR COMPLEX TASKS
                
                The manager may request actions that require several steps or multiple tools to be chained together. Never hesitate to combine the available tools to fully address the request.
                For example:
                
                "Cancel all radiology appointments" → first use the event search tool to find all matching events, then call the cancellation tool for each of them.
                "Show me the consultations for next week" → use the subject-based search tool combined with the date-range search tool to return only the relevant events for that period.
                "Free up Friday morning and move the 10 AM appointment to 2 PM" → first identify the relevant events using the consultation tool, then perform the actions in a logical order.
                
                Always break down a complex request into a coherent sequence of tool calls before responding, and clearly summarize all actions performed to the manager at the end.
                
                For any action that modifies or cancels an appointment, first retrieve the necessary information (such as the event ID) using the appropriate consultation tool before taking action, and clearly confirm the action performed to the manager.
                
                CONVERSATION MEMORY USAGE
                
                If the manager asks for the list of appointments on a specific date and then asks the same question again less than one minute later, respond directly using the information from the previous exchange without calling the tool again. If more than one minute has elapsed since the last consultation for that date, call the corresponding tool again to retrieve up-to-date data, as the schedule may have changed in the meantime.
        """.formatted(
                maintenant.toLocalDate().format(formatterDate),
                maintenant.toLocalTime().format(formatterHeure),
                ouverture.format(formatterHeure),
                fermeture.format(formatterHeure)
        );

        return chatClient.prompt()
                .system(systemPrompt)
                .user(query)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                .stream()
                .content();
    }
}
