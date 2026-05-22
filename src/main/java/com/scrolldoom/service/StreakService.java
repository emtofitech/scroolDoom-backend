package com.scrolldoom.service;

import com.scrolldoom.dto.StreakResponse;
import com.scrolldoom.model.Streak;
import com.scrolldoom.repository.BreachEventRepository;
import com.scrolldoom.repository.StreakRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

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
        Date yesterdayStart = toDateStart(yesterday);
        Date yesterdayEnd = toDateEnd(yesterday);

        boolean hadBreachYesterday = breachEventRepository
                .existsByUserIdAndBreachedAtBetween(userId, yesterdayStart, yesterdayEnd);

        if (!hadBreachYesterday) {
            LocalDate dayBeforeYesterday = yesterday.minusDays(1);
            if (dayBeforeYesterday.equals(streak.getLastSuccessDate())) {
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
            } else {
                streak.setCurrentStreak(1);
            }
            streak.setLastSuccessDate(yesterday);
            if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                streak.setLongestStreak(streak.getCurrentStreak());
            }
        } else {
            streak.setCurrentStreak(0);
        }

        streak.setUpdatedAt(new Date());
        return mapToResponse(streakRepository.save(streak));
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
