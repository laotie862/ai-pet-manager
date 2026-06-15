package com.example.demo.report;

import com.example.demo.behavior.BehaviorEventRecord;
import com.example.demo.behavior.BehaviorRepository;
import com.example.demo.behavior.BehaviorSummaryResponse;
import com.example.demo.common.api.ErrorCode;
import com.example.demo.common.exception.BusinessException;
import com.example.demo.common.security.SecurityUtils;
import com.example.demo.pet.PetRecord;
import com.example.demo.pet.PetRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class DailyReportService {
    private final PetRepository petRepository;
    private final BehaviorRepository behaviorRepository;
    private final WeatherClient weatherClient;
    private final QwenReportClient qwenReportClient;
    private final ReportProperties reportProperties;

    public DailyReportService(
            PetRepository petRepository,
            BehaviorRepository behaviorRepository,
            WeatherClient weatherClient,
            QwenReportClient qwenReportClient,
            ReportProperties reportProperties
    ) {
        this.petRepository = petRepository;
        this.behaviorRepository = behaviorRepository;
        this.weatherClient = weatherClient;
        this.qwenReportClient = qwenReportClient;
        this.reportProperties = reportProperties;
    }

    public DailyReportResponse generate(Long petId, LocalDate date) {
        Long userId = SecurityUtils.currentUser().id();
        PetRecord pet = petRepository.findByIdAndUser(petId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Pet not found"));
        LocalDate reportDate = date == null ? LocalDate.now(ZoneOffset.UTC) : date;
        BehaviorSummaryResponse summary = behaviorRepository.summary(petId, reportDate)
                .orElseGet(() -> BehaviorSummaryResponse.empty(petId, reportDate));
        List<BehaviorEventRecord> events = behaviorRepository.timeline(petId, reportDate);
        WeatherSnapshot weather = weatherClient.today();
        String prompt = buildPrompt(pet, summary, events, weather);
        String template = templateReport(pet, summary, events, weather, reportDate);
        String content = qwenReportClient.generate(prompt).orElse(template);
        boolean templateFallback = content.equals(template);

        return new DailyReportResponse(
                pet.id(),
                pet.name(),
                reportDate,
                content,
                prompt,
                weather,
                summary,
                templateFallback,
                templateFallback ? "template-fallback" : "qwen:" + reportProperties.getQwenModel()
        );
    }

    private String buildPrompt(
            PetRecord pet,
            BehaviorSummaryResponse summary,
            List<BehaviorEventRecord> events,
            WeatherSnapshot weather
    ) {
        int activeMinutes = minutes(summary.exercisingSeconds());
        int sleepMinutes = minutes(summary.sleepingSeconds());
        return """
                请根据下面的结构化数据，生成一段给宠物主人的每日宠物报告。
                要求：
                1. 用中文，语气温和自然，不要像表格。
                2. 明确说出宠物年龄、品种/物种、今天进食几次、喝水几次、活动几分钟、睡眠几分钟、排便几次。
                3. 给出健康情况观察，但不要做医学诊断，不要替代兽医。
                4. 结合天气给出照护建议。
                5. 如果数据偏少，要提醒用户继续观察，不要编造不存在的数据。
                6. 控制在 120 到 180 字。

                宠物资料：
                名字：%s
                年龄：%s
                物种：%s
                品种：%s
                体重：%s

                今日行为数据：
                进食：%d 次
                饮水：%d 次
                活动：%d 分钟
                睡眠：%d 分钟
                排便：%d 次
                行为事件总数：%d

                天气：
                城市：%s
                状况：%s
                温度：%s
                天气建议：%s
                """.formatted(
                pet.name(),
                ageText(pet),
                blankOr(pet.species(), "未填写"),
                blankOr(pet.breed(), "未填写"),
                pet.weightKg() == null ? "未填写" : pet.weightKg() + "kg",
                summary.eatingCount(),
                summary.drinkingCount(),
                activeMinutes,
                sleepMinutes,
                summary.defecatingCount(),
                events.size(),
                weather.city(),
                weather.condition(),
                weather.temperatureCelsius() == null ? "未知" : weather.temperatureCelsius() + "℃",
                weather.advice()
        );
    }

    private String templateReport(
            PetRecord pet,
            BehaviorSummaryResponse summary,
            List<BehaviorEventRecord> events,
            WeatherSnapshot weather,
            LocalDate date
    ) {
        int activeMinutes = minutes(summary.exercisingSeconds());
        int sleepMinutes = minutes(summary.sleepingSeconds());
        String health = healthHint(summary, events.size());
        return "%s在%s的记录如下：%s，%s，今天进食%d次、饮水%d次、活动约%d分钟、睡眠约%d分钟、排便%d次。%s%s"
                .formatted(
                        pet.name(),
                        date,
                        ageText(pet),
                        petDescription(pet),
                        summary.eatingCount(),
                        summary.drinkingCount(),
                        activeMinutes,
                        sleepMinutes,
                        summary.defecatingCount(),
                        health,
                        weather.available()
                                ? " 今日" + weather.city() + "天气" + weather.condition() + "，" + weather.advice()
                                : " " + weather.advice()
                );
    }

    private String healthHint(BehaviorSummaryResponse summary, int eventCount) {
        if (eventCount == 0) {
            return "今天行为数据还比较少，建议继续观察饮水、进食和精神状态。";
        }
        if (summary.drinkingCount() == 0) {
            return "今天暂未记录到饮水，请留意水碗变化和实际饮水情况。";
        }
        if (minutes(summary.exercisingSeconds()) == 0 && minutes(summary.sleepingSeconds()) > 180) {
            return "今天整体偏安静，如果精神、食欲正常通常可以继续观察。";
        }
        return "整体行为记录较稳定，可继续保持规律喂食、饮水和活动观察。";
    }

    private String petDescription(PetRecord pet) {
        String species = blankOr(pet.species(), "宠物");
        String breed = blankOr(pet.breed(), "");
        return breed.isBlank() ? species : breed + species;
    }

    private String ageText(PetRecord pet) {
        if (pet.birthday() == null) {
            return "年龄未填写";
        }
        Period age = Period.between(pet.birthday(), LocalDate.now(ZoneOffset.UTC));
        if (age.getYears() > 0) {
            return age.getYears() + "岁" + (age.getMonths() > 0 ? age.getMonths() + "个月" : "");
        }
        if (age.getMonths() > 0) {
            return age.getMonths() + "个月";
        }
        return Math.max(0, age.getDays()) + "天";
    }

    private int minutes(int seconds) {
        return Math.round(seconds / 60.0f);
    }

    private String blankOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
