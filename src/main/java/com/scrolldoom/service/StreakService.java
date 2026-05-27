package com.scrolldoom.service;

import com.scrolldoom.dto.StreakResponse;
import com.scrolldoom.model.BreachEvent;
import com.scrolldoom.model.Streak;
import com.scrolldoom.repository.BreachEventRepository;
import com.scrolldoom.repository.StreakRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StreakService {

    private final StreakRepository streakRepository;
    private final BreachEventRepository breachEventRepository;

    public StreakService(StreakRepository streakRepository,
                         BreachEventRepository breachEventRepository) {
        this.streakRepository = streakRepository;
        this.breachEventRepository = breachEventRepository;
    }

    public StreakResponse getOrCalculateStreak(ObjectId userId) {
        Streak streak = streakRepository.findByUserId(userId)
                .orElse(Streak.builder().userId(userId).build());

        LocalDate today = LocalDate.now();
        if (streak.getUpdatedAt() != null && isSameDay(streak.getUpdatedAt(), today)) {
            return mapToResponse(streak);
        }

        LocalDate yesterday = today.minusDays(1);

        boolean hadBreachYesterday = breachEventRepository
                .existsByUserIdAndBreachedAtBetween(userId, toDateStart(yesterday), toDateEnd(yesterday));

        if (hadBreachYesterday) {
            streak.setCurrentStreak(0);
        } else {
            int consecutiveDays = countConsecutiveCleanDays(userId, yesterday, streak.getLastSuccessDate());
            streak.setCurrentStreak(consecutiveDays);
            streak.setLastSuccessDate(yesterday);
            if (consecutiveDays > streak.getLongestStreak()) {
                streak.setLongestStreak(consecutiveDays);
            }
        }

        streak.setUpdatedAt(new Date());
        return mapToResponse(streakRepository.save(streak));
    }

    private int countConsecutiveCleanDays(ObjectId userId, LocalDate fromDate, LocalDate lastSuccessDate) {
        if (lastSuccessDate != null && lastSuccessDate.equals(fromDate)) {
            return 1;
        }

        LocalDate searchFrom = (lastSuccessDate != null && lastSuccessDate.isBefore(fromDate))
                ? lastSuccessDate.plusDays(1)
                : fromDate.minusDays(30);

        List<BreachEvent> breaches = breachEventRepository.findByUserIdAndBreachedAtBetween(
                userId, toDateStart(searchFrom), toDateEnd(fromDate));

        Set<LocalDate> breachDates = breaches.stream()
                .map(b -> b.getBreachedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .collect(Collectors.toSet());

        int count = 0;
        LocalDate day = fromDate;
        while (!day.isBefore(searchFrom) && !breachDates.contains(day)) {
            count++;
            day = day.minusDays(1);
        }

        return count;
    }

    private boolean isSameDay(Date date, LocalDate localDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR) == localDate.getYear()
                && cal.get(Calendar.DAY_OF_YEAR) == localDate.getDayOfYear();
    }

    private Date toDateStart(LocalDate localDate) {
        Calendar cal = Calendar.getInstance();
        cal.set(localDate.getYear(), localDate.getMonthValue() - 1, localDate.getDayOfMonth(),
                0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date toDateEnd(LocalDate localDate) {
        Calendar cal = Calendar.getInstance();
        cal.set(localDate.getYear(), localDate.getMonthValue() - 1, localDate.getDayOfMonth(),
                23, 59, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    private StreakResponse mapToResponse(Streak streak) {
        return StreakResponse.builder()
                .currentStreak(streak.getCurrentStreak())
                .longestStreak(streak.getLongestStreak())
                .lastSuccessDate(streak.getLastSuccessDate())
                .updatedAt(streak.getUpdatedAt())
                .build();
    }
}
