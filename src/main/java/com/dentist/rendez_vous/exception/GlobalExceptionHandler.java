package com.dentist.rendez_vous.exception;

import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Form validation errors (@Valid + @ModelAttribute)
    @ExceptionHandler(BindException.class)
    public ModelAndView handleValidationErrors(BindException ex) {
        ModelAndView mav = new ModelAndView("rendezvous-form");
        String objectName = ex.getBindingResult().getObjectName();

        // Returns the submitted object (with the values already entered)
        mav.addObject(objectName, ex.getTarget());

        // Returns the errors under the key expected by Thymeleaf for th:errors
        mav.addObject(
                BindingResult.MODEL_KEY_PREFIX + objectName,
                ex.getBindingResult()
        );

        return mav;
    }

    @ExceptionHandler(CreneauIndisponibleException.class)
    public ModelAndView handleCreneauIndisponible(CreneauIndisponibleException ex) {
        ModelAndView mav = new ModelAndView("rendezvous-form");
        mav.addObject("errorMessage", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(GoogleCalendarException.class)
    public ModelAndView handleGoogleCalendarError(GoogleCalendarException ex) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject(
                "message",
                "Communication error with Google Calendar: " + ex.getMessage()
        );
        return mav;
    }

    @ExceptionHandler(EvenementNotFoundException.class)
    public ModelAndView handleEvenementNotFound(EvenementNotFoundException ex) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(AucunEvenementTrouveException.class)
    public ModelAndView handleAucunEvenementTrouve(AucunEvenementTrouveException ex) {
        ModelAndView mav = new ModelAndView("rendezvous-liste"); // Your results display view
        mav.addObject("infoMessage", ex.getMessage());
        mav.addObject("evenements", Collections.emptyList());
        return mav;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgument(IllegalArgumentException ex) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(WeekendException.class)
    public ModelAndView handleWeekend(WeekendException ex) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(TokenInvalideException.class)
    public ModelAndView handleTokenInvalide(TokenInvalideException ex) {
        ModelAndView mav = new ModelAndView("token-invalide");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler({
            SessionExpireeException.class,
            EmailNonVerifieException.class,
            UserHaveEncorsEventException.class
    })
    public String handleSessionExpiree(
            RuntimeException ex,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/rdv/patient";
    }

    @ExceptionHandler(UserHaveArledyEventException.class)
    public ModelAndView handelUserExsite(UserHaveArledyEventException ex) {
        ModelAndView mav = new ModelAndView("email");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(OurTimeConditionException.class)
    public ModelAndView handelTime(OurTimeConditionException ex) {
        ModelAndView mav = new ModelAndView("time");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    @ExceptionHandler(LogicalDateTime.class)
    public ModelAndView handelDate(LogicalDateTime ex) {
        ModelAndView mav = new ModelAndView("date");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    // Any other unexpected exception (database, external service, etc.)
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(Exception ex) {
        ModelAndView mav = new ModelAndView("error");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

}