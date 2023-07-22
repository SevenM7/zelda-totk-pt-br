package org.example.database;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.example.database.gpt.GPTTranslate;
import org.example.utils.ExecutorServiceUtils;
import org.example.utils.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TranslateByGPT {
    private int total = 0;
    private int completed = 0;
    private final int limitBuffer = 5000;

    private final EasyDatabaseManager dbManager;
    private final GPTTranslate gptTranslate;

    public static void main(String[] args) {
        TranslateByGPT translateByGPT = new TranslateByGPT();
        translateByGPT.start();
    }

    public TranslateByGPT() {
        dbManager = new EasyDatabaseManager();
        gptTranslate = new GPTTranslate();
        total = retriveTotal();
        completed = retriveCompletedTranslated();
    }

    public void start() {
        // scheduler resume
        ExecutorServiceUtils.SCHEDULER.scheduleAtFixedRate(this::printReport, 1, 1, TimeUnit.MINUTES);

        while (true) {

            var bufferTableString = retriveBuffer();

            if (bufferTableString.size() == 0) {
                log.info("Buffer is empty, exiting...");
                System.exit(0);
            }

            processStringTable(bufferTableString);
        }
    }

    private void processStringTable(Map<Integer, String> stringTableBuffer) {
        List<Map<Integer, String>> bufferStringTableSplitted = splitStringTableByLimitChar(stringTableBuffer);

        log.info("String table splitted in " + bufferStringTableSplitted.size() + " parts");

        List<CompletableFuture<Map<Integer, String>>> futures = new ArrayList<>();

        int sizeTable = bufferStringTableSplitted.size();
        var indexAtomic = new AtomicInteger(0);

        for (int i = 0; i < bufferStringTableSplitted.size(); i++) {
            Map<Integer, String> buffer = bufferStringTableSplitted.get(i);
            futures.add(translateBufferAsync(buffer, indexAtomic, sizeTable));
        }

        try {
            // ignore result with error
            futures.forEach(future -> {
                try {
                    future.join();
                } catch (Exception e) {
                    log.error("Error on translate buffer", e);
                }
            });
        } catch (Exception e) {
            log.error("Error on translate buffer", e);

            log.info("Wait 1 minute to try again");
            try {
                Thread.sleep(60000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }

        log.info("Completed: " + completed + " of " + total);
    }

    static String[] PLACEHOLDER = new String[] {
        "\\u000E\\u0004\\u0000$\"CustomSavsaaba_00",
        "\\u000E\\u0004\\u0000 \\u001ETalkPositive_03",
        "\\u000E\\u0004\\u0000 \\u001ETalkPositive_04",
        "\\u000E\\u0004\\u0000\\u0018\\u0016ShidoClench",
        "\\u000E\\u0004\\u0000 \\u001ETalkPositive_00",
        "\\u000E\\u0004\\u0000 \\u001ETalkPositive_01",
        "\\u000E\\u0004\\u0000 \\u001ETalkPositive_02",
        "\\u000E\\u0004\\u0000\\u001C\\u001AAdmiration_03",
        "\\u000E\\u0004\\u0000(&QuestionSurprise_01",
        "\\u000E\\u0004\\u0000(&QuestionSurprise_00",
        "\\u000E\\u0004\\u0000\\u001C\\u001AAdmiration_00",
        "\\u000E\\u0004\\u0000\\u0010\\u000EPainful",
        "\\u000E\\u0004\\u0000\\u001C\\u001ASakuradaCM_02",
        "\\u000E\\u0004\\u0000\\u001C\\u001AAdmiration_01",
        "\\u000E\\u0004\\u0000(&PurahTalkSerious_00",
        "\\u000E\\u0004\\u0000(&PurahTalkSerious_01",
        "\\u000E\\u0004\\u0000,*MiloyanTactPlayEnd_00",
        "\\u000E\\u0004\\u0000\\u0010\\u000ETalk_01",
        "\\u000E\\u0004\\u0000\\u0010\\u000ETalk_04",
        "\\u000E\\u0004\\u0000\\u0010\\u000EGoodbye",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Eating_00",
        "\\u000E\\u0004\\u0000\\u0014\\u0012SurpriseM",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Eating_01",
        "\\u000E\\u0004\\u0000\\u0014\\u0012SurpriseL",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Tension_00",
        "\\u000E\\u0004\\u0000\\u0018\\u0016TuliHowl_00",
        "\\u000E\\u0004\\u0000\\u0014\\u0012SickThink",
        "\\u000E\\u0004\\u0000\\u001A\\u0018TuliAngry_00",
        "\\u000E\\u0004\\u0000\\u001A\\u0018WorkHeavy_01",
        "\\u000E\\u0004\\u0000\\u0014\\u0012SurpriseS",
        "\\u000E\\u0004\\u0000 \\u001EWeaponSwingS_01",
        "\\u000E\\u0004\\u0000\\u001A\\u0018WorkHeavy_00",
        "\\u000E\\u0004\\u0000\\u000C\\u000AThink",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Wakeup_00",
        "\\u000E\\u0004\\u0000\\u001A\\u0018CustomVasaaq",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Wakeup_01",
        "\\u000E\\u0004\\u0000\\u001C\\u001AUnderstand_01",
        "\\u000E\\u0004\\u0000\\u001C\\u001AUnderstand_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CUnconscious_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CUnconscious_01",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Runaway_00",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Runaway_01",
        "\\u000E\\u0004\\u0000\\u0018\\u0016CustomHappy",
        "\\u000E\\u0004\\u0000$\"QuestionNormal_00",
        "\\u000E\\u0004\\u0000$\"QuestionNormal_01",
        "\\u000E\\u0004\\u0000\\u001C\\u001ATuliSpirit_00",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Angry_01",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Angry_00",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Tired_00",
        "\\u000E\\u0004\\u0000\\u000E\\u000CEating",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Tired_01",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Guess_00",
        "\\u000E\\u0004\\u0000\\u001A\\u0018CustomSavaaq",
        "\\u000E\\u0004\\u0000*(ZeldaTalkPositive_01",
        "\\u000E\\u0004\\u0000\\u001C\\u001AIrritation_00",
        "\\u000E\\u0004\\u0000\" ZeldaSurprise_03",
        "\\u000E\\u0004\\u0000\\u0010\\u000EPass_03",
        "\\u000E\\u0004\\u0000\\u0010\\u000EPass_00",
        "\\u000E\\u0004\\u0000\" QuestionAngry_01",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Understand",
        "\\u000E\\u0004\\u0000\" QuestionAngry_00",
        "\\u000E\\u0004\\u0000,*HiddenKorok_Appear_00",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Pointing_00",
        "\\u000E\\u0004\\u0000\\u001C\\u001AHysterical_00",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Pointing_03",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Pointing_02",
        "\\u000E\\u0004\\u0000\\u0018\\u0016LaughBad_00",
        "\\u000E\\u0004\\u0000\\u0018\\u0016LaughBad_01",
        "\\u000E\\u0004\\u0000\\u0016\\u0014CallFar_01",
        "\\u000E\\u0004\\u0000\\u0010\\u000EEase_01",
        "\\u000E\\u0004\\u0000\\u0016\\u0014CallFar_00",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Happy_03",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Happy_02",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Happy_01",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Happy_00",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Greeting_01",
        "\\u000E\\u0004\\u0000\\u0010\\u000EEase_00",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Greeting_00",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Greeting_03",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Greeting_02",
        "\\u000E\\u0004\\u0000\\u0010\\u000EWork_00",
        "\\u000E\\u0004\\u0000\\u0010\\u000EWork_01",
        "\\u000E\\u0004\\u0000\" YunBoArrogant_00",
        "\\u000E\\u0004\\u0000$\"ArrogantLaughS_00",
        "\\u000E\\u0004\\u0000\\u0014\\u0012LaughS_01",
        "\\u000E\\u0004\\u0000\\u0014\\u0012LaughS_00",
        "\\u000E\\u0004\\u0000\\u0010\\u000ESavo_00",
        "\\u000E\\u0004\\u0000\" YunBoGreeting_01",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Admiration",
        "\\u000E\\u0004\\u0000\" YunBoGreeting_00",
        "\\u000E\\u0004\\u0000\\u0016\\u0014CustomUrge",
        "\\u000E\\u0004\\u0000 \\u001ECustomVasaaq_00",
        "\\u000E\\u0004\\u0000\\u001A\\u0018Encourage_00",
        "\\u000E\\u0004\\u0000\\u0010\\u000ECare_01",
        "\\u000E\\u0004\\u0000\\u0010\\u000ECare_02",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Taunt_00",
        "\\u000E\\u0004\\u0000\\u0014\\u0012LaughM_01",
        "\\u000E\\u0004\\u0000\\u0010\\u000ETrouble",
        "\\u000E\\u0004\\u0000\\u001A\\u0018Nostalgia_06",
        "\\u000E\\u0004\\u0000\" Determination_01",
        "\\u000E\\u0004\\u0000\" Determination_00",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Puzzled_00",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Think_01",
        "\\u000E\\u0004\\u0000\\u000E\\u000CNotice",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Think_02",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Think_03",
        "\\u000E\\u0004\\u0000\\u001C\\u001AQuestionAngry",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Think_00",
        "\\u000E\\u0004\\u0000\\u001A\\u0018Nostalgia_05",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Painful_01",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Painful_00",
        "\\u000E\\u0004\\u0000\\u001A\\u0018Nostalgia_02",
        "\\u000E\\u0004\\u0000\\u001A\\u0018Nostalgia_01",
        "\\u000E\\u0004\\u0000$\"ShidoImpressed_00",
        "\\u000E\\u0004\\u0000\\u001A\\u0018Custom006_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CShidoClench_01",
        "\\u000E\\u0004\\u0000\\u000E\\u000CSad_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CShidoClench_00",
        "\\u000E\\u0004\\u0000\\u000A\\u0008Pass",
        "\\u000E\\u0004\\u0000\\u000E\\u000CSad_01",
        "\\u000E\\u0004\\u0000\\u0012\\u0010LaughBad",
        "\\u000E\\u0004\\u0000\\u000C\\u000ATired",
        "\\u000E\\u0004\\u0000\\u001A\\u0018SurpriseM_00",
        "\\u000E\\u0004\\u0000\\u001A\\u0018SurpriseM_01",
        "\\u000E\\u0004\\u0000 \\u001EShidoAizuchi_00",
        "\\u000E\\u0004\\u0000 \\u001EShidoAizuchi_01",
        "\\u000E\\u0004\\u0000\\u001C\\u001ATuliNotice_00",
        "\\u000E\\u0004\\u0000.,ZeldaQuestionNormal_01",
        "\\u000E\\u0004\\u0000\\u001E\\u001CCustomSavsaaba",
        "\\u000E\\u0004\\u0000\\u0016\\u0014CustomCold",
        "\\u000E\\u0004\\u0000\\u0010\\u000ERunaway",
        "\\u000E\\u0004\\u0000\\u000A\\u0008Sigh",
        "\\u000E\\u0004\\u0000$\"SageSkillStart_00",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Scare_01",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Scare_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CCustomBigHappy",
        "\\u000E\\u0004\\u0000\\u000E\\u000CJoy_03",
        "\\u000E\\u0004\\u0000\\u000E\\u000CJoy_04",
        "\\u000E\\u0004\\u0000\\u000E\\u000CJoy_05",
        "\\u000E\\u0004\\u0000\" QuestionSurprise",
        "\\u000E\\u0004\\u0000\\u000E\\u000CJoy_01",
        "\\u000E\\u0004\\u0000\\u000A\\u0008Ease",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Rocket_02",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Sleep_00",
        "\\u000E\\u0004\\u0000\\u001C\\u001ACustomSavotta",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Rocket_00",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Sleep_01",
        "\\u000E\\u0004\\u0000\\u0014\\u0012LaughL_00",
        "\\u000E\\u0004\\u0000\\u0014\\u0012LaughL_01",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Rocket_04",
        "\\u000E\\u0004\\u0000\\u001A\\u0018CustomYouTsu",
        "\\u000E\\u0004\\u0000\\u001A\\u0018Convinced_00",
        "\\u000E\\u0004\\u0000\\u000C\\u000AScare",
        "\\u000E\\u0004\\u0000\\u001E\\u001CInspiration_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CCustomBadThink",
        "\\u000E\\u0004\\u0000\\u001A\\u0018ShoutTalk_01",
        "\\u000E\\u0004\\u0000\" TuliDepressed_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CQuestionNormal",
        "\\u000E\\u0004\\u0000\\u001A\\u0018Encourage_01",
        "\\u000E\\u0004\\u0000\\u001A\\u0018SurpriseS_01",
        "\\u000E\\u0004\\u0000\\u001A\\u0018Discovery_04",
        "\\u000E\\u0004\\u0000\\u001A\\u0018SurpriseS_00",
        "\\u000E\\u0004\\u0000\\u000A\\u0008Savo",
        "\\u000E\\u0004\\u0000\\u001A\\u0018SickCough_00",
        "\\u000E\\u0004\\u0000\\u000A\\u0008Sava",
        "\\u000E\\u0004\\u0000\\u001A\\u0018SickThink_01",
        "\\u000E\\u0004\\u0000\\u001A\\u0018SickThink_00",
        "\\u000E\\u0004\\u0000\\u0010\\u000EHumming",
        "\\u000E\\u0004\\u0000\\u000E\\u000CRun_01",
        "\\u000E\\u0004\\u0000\\u000E\\u000CRun_00",
        "\\u000E\\u0004\\u0000\\u001C\\u001ADetermination",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Surprise_00",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Surprise_03",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Arrogant_00",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Arrogant_01",
        "\\u000E\\u0004\\u0000\\u0014\\u0012AngryL_00",
        "\\u000E\\u0004\\u0000\\u000C\\u000AHappy",
        "\\u000E\\u0004\\u0000\\u0014\\u0012AngryL_01",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Goodbye_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CObservation_01",
        "\\u000E\\u0004\\u0000\\u001E\\u001CObservation_02",
        "\\u000E\\u0004\\u0000\\u001E\\u001CRegrettable_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CRegrettable_01",
        "\\u000E\\u0004\\u0000\\u001E\\u001CRegrettable_02",
        "\\u000E\\u0004\\u0000\\u001E\\u001CRegrettable_03",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Beware_04",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Beware_00",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Humming_00",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Humming_01",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Beware_02",
        "\\u000E\\u0004\\u0000\\u000C\\u000AGuess",
        "\\u000E\\u0004\\u0000\\u001E\\u001CCustomHappy_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CCustomHappy_01",
        "\\u000E\\u0004\\u0000\\u001E\\u001CObservation_00",
        "\\u000E\\u0004\\u0000\" CustomSavotta_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CSagonoCheck_03",
        "\\u000E\\u0004\\u0000\\u001E\\u001CSagonoCheck_02",
        "\\u000E\\u0004\\u0000\\u001E\\u001CSagonoCheck_01",
        "\\u000E\\u0004\\u0000\\u001E\\u001CSagonoCheck_00",
        "\\u000E\\u0004\\u0000\\u0014\\u0012WorkHeavy",
        "\\u000E\\u0004\\u0000\\u0010\\u000ECallFar",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Shitauchi",
        "\\u000E\\u0004\\u0000\\u000A\\u0008Chat",
        "\\u000E\\u0004\\u0000\\u001E\\u001CArrogantLaughS",
        "\\u000E\\u0004\\u0000\\u001A\\u0018Shitauchi_00",
        "\\u000E\\u0004\\u0000\\u001A\\u0018Shitauchi_01",
        "\\u000E\\u0004\\u0000\\u0010\\u000ESigh_00",
        "\\u000E\\u0004\\u0000\" PurahGreeting_00",
        "\\u000E\\u0004\\u0000\\u0010\\u000ESigh_01",
        "\\u000E\\u0004\\u0000\\u001E\\u001CDescription_00",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Notice_01",
        "\\u000E\\u0004\\u0000 \\u001ECustomSavaaq_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CDescription_02",
        "\\u000E\\u0004\\u0000\\u001E\\u001CDescription_03",
        "\\u000E\\u0004\\u0000\\u0014\\u0012Notice_00",
        "\\u000E\\u0004\\u0000 \\u001ESickGreeting_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CShidoSpirit_00",
        "\\u000E\\u0004\\u0000\\u0012\\u0010TalkDeny",
        "\\u000E\\u0004\\u0000\\u0010\\u000ESava_00",
        "\\u000E\\u0004\\u0000\\u0008\\u0006Cry",
        "\\u000E\\u0004\\u0000\\u000C\\u000AAngry",
        "\\u000E\\u0004\\u0000\\u001A\\u0018SurpriseL_01",
        "\\u000E\\u0004\\u0000 \\u001EWeaponSwingM_02",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Unconscious",
        "\\u000E\\u0004\\u0000\\u001A\\u0018SurpriseL_00",
        "\\u000E\\u0004\\u0000\\u0010\\u000EChat_01",
        "\\u000E\\u0004\\u0000\\u001C\\u001AGroundwork_00",
        "\\u000E\\u0004\\u0000\\u001A\\u0018TalkPositive",
        "\\u000E\\u0004\\u0000\" PurahTalkSerious",
        "\\u000E\\u0004\\u0000 \\u001EWeaponSwingM_00",
        "\\u000E\\u0004\\u0000\\u0010\\u000EChat_00",
        "\\u000E\\u0004\\u0000 \\u001EShidoInspire_00",
        "\\u000E\\u0004\\u0000\\u000C\\u000ASleep",
        "\\u000E\\u0004\\u0000\\u0012\\u0010Greeting",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Trouble_01",
        "\\u000E\\u0004\\u0000\\u0016\\u0014Trouble_00",
        "\\u000E\\u0004\\u0000\\u000E\\u000CLaughL",
        "\\u000E\\u0004\\u0000\\u000E\\u000CCry_00",
        "\\u000E\\u0004\\u0000\\u0008\\u0006Sad",
        "\\u000E\\u0004\\u0000\\u0018\\u0016TalkDeny_00",
        "\\u000E\\u0004\\u0000 \\u001ETuliPositive_00",
        "\\u000E\\u0004\\u0000\\u001E\\u001CEmbarrassed_00",
        "\\u000E\\u0004\\u0000\\u0018\\u0016TalkDeny_01",
        "\\u000E\\u0004\\u0000\\u001E\\u001CEmbarrassed_01",
        "\\u000E\\u0004\\u0000 \\u001ESickGreeting_01",
        "\\u000E\\u0004\\u0000\\u0018\\u0016Inspiration",
        "\\u000E\\u0004\\u0000\\u000E\\u000CWakeup",
        "\\u000E\\u0004\\u0000\\u000E\\u000CLaughS",
        "\\u000E\\u0004\\u0000\\u001C\\u001AGroundwork_01",
        "\\u000E\\u0004\\u0000\\u000A\\u0008Work"
    };



    private String replacePlaceholder(String str) {
        for (int i = 0; i < PLACEHOLDER.length; i++) {
            if (str.contains(PLACEHOLDER[i])) {
                str = str.replace(PLACEHOLDER[i], "{FT" + i + "}");
            }
        }

        return str;
    }

    private String replacePlaceholderBack(String str) {
        for (int i = 0; i < PLACEHOLDER.length; i++) {
            if (str.contains("{FT" + i + "}")) {
                str = str.replace("{FT" + i + "}", PLACEHOLDER[i]);
            }
        }

        return str;
    }

    private CompletableFuture<Map<Integer, String>> translateBufferAsync(final Map<Integer, String> bufferStringTable, final AtomicInteger indexBuffer, final int totalBuffer) {
        CompletableFuture<Map<Integer, String>> future = new CompletableFuture<>();

        CompletableFuture.runAsync(() -> {
            try {
                var newBufferStringTable = new HashMap<Integer, String>();

                for (var entry : bufferStringTable.entrySet()) {
                    newBufferStringTable.put(entry.getKey(), replacePlaceholder(entry.getValue()));
                }

                translateBuffer(newBufferStringTable).whenCompleteAsync((result, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                        return;
                    }

                    var newResult = new HashMap<Integer, String>();

                    for (var entry : result.entrySet()) {
                        newResult.put(entry.getKey(), replacePlaceholderBack(entry.getValue()));
                    }

                    updateBuffer(bufferStringTable, newResult);
                    int index = indexBuffer.incrementAndGet();
                    log.info("Buffer " + (index + 1) + " of " + totalBuffer + " translated successfully (" + result.size() + " strings)");

                    future.complete(newResult);
                });
            }
            catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, ExecutorServiceUtils.EXECUTOR);

        return future;
    }

    private CompletableFuture<Map<Integer, String>> translateBuffer(Map<Integer, String> bufferStringTable) throws IOException {
        return gptTranslate.translateStringTable(bufferStringTable);
    }

    private int retriveCompletedTranslated() {
        String query = "SELECT count(*) as total FROM msbt_text.text_table WHERE translated = true";
        return dbManager.singleResult(query, resultSet -> resultSet.getInt("total"));
    }

    private int retriveTotal() {
        String query = "SELECT count(*) as total FROM msbt_text.text_table";
        return dbManager.singleResult(query, resultSet -> resultSet.getInt("total"));
    }

    public Map<Integer, String> retriveBuffer() {
        log.info("Retriving buffer...");

        String query = "SELECT id, original_text FROM msbt_text.labels_text_2 WHERE translated = false order by id limit ?";
        List<Map.Entry<Integer, String>> buffer = dbManager.queryList(query,
            statement -> {
                statement.setInt(1, limitBuffer);
            },

            resultSet -> {
                return new HashMap.SimpleEntry<>(resultSet.getInt("id"), resultSet.getString("original_text"));
            }
        );

        HashMap<Integer, String> bufferStringTable = new HashMap<>();

        for (Map.Entry<Integer, String> entry : buffer) {
            bufferStringTable.put(entry.getKey(), entry.getValue());
        }

        log.info("Buffer retrived, size: " + bufferStringTable.size());

        return bufferStringTable;
    }

    private void updateBuffer(Map<Integer, String> originalStringTable, Map<Integer, String> bufferStringTranslated) {
        log.debug("Updating buffer...");
        sanitizeBuffer(bufferStringTranslated);
        checkBuffer(originalStringTable, bufferStringTranslated);

        String query = "UPDATE msbt_text.labels_text_2 SET translated_text = ?, translated = true WHERE id = ?";

        dbManager.batchUpdate(query, statement -> {
            for (Map.Entry<Integer, String> entry : bufferStringTranslated.entrySet()) {
                statement.setString(1, entry.getValue());
                statement.setInt(2, entry.getKey());
                statement.addBatch();
            }
        });
    }

    private void sanitizeBuffer(Map<Integer, String> bufferStringTranslated) {
        for (Map.Entry<Integer, String> entry : bufferStringTranslated.entrySet()) {
            String stringTranslated = entry.getValue();
            String stringSanitized = StringUtils.sanitizeString(stringTranslated);
            bufferStringTranslated.put(entry.getKey(), stringSanitized);
        }
    }

    private void checkBuffer(Map<Integer, String> originalStringTable, Map<Integer, String> bufferStringTranslated) {
        for (Map.Entry<Integer, String> entry : originalStringTable.entrySet()) {
            String stringTranslated = bufferStringTranslated.get(entry.getKey());
            String stringOriginal = entry.getValue();

            if (stringTranslated == null) {
                throw new RuntimeException("String translated is null, id: " + entry.getKey() + ", string: " + stringOriginal);
            }

//            log.info("Operation: " + stringOriginal + " - " + stringTranslated);
        }
    }

    private void printReport() {
        String query = "select count(*) as total_lines, sum(LENGTH(original_text)) as total_char FROM msbt_text.text_table WHERE translated = false";

        dbManager.singleResult(query, resultSet -> {
            int totalLines = resultSet.getInt("total_lines");
            int totalChar = resultSet.getInt("total_char");

            log.info("Summary: " + totalLines + " lines, " + totalChar + " char");

            return -1;
        });
    }

    private List<Map<Integer, String>> splitStringTableByLimitChar(Map<Integer, String> bufferStringTable) {
        int charLimit = 1700;

        List<Map<Integer, String>> newBufferStringTable = new ArrayList<>();
        Set<Map.Entry<Integer, String>> entries = bufferStringTable.entrySet();

        int charCount = 0;

        HashMap<Integer, String> currentMap = new HashMap<>();
        newBufferStringTable.add(currentMap);

        for (Map.Entry<Integer, String> entry : entries) {
            Integer key = entry.getKey();
            String value = entry.getValue();

            charCount += value.length() + 2;

            if (charCount >= charLimit) {
                currentMap = new HashMap<>();
                newBufferStringTable.add(currentMap);
                charCount = 0;
            }

            currentMap.put(key, value);
        }

        return newBufferStringTable;
    }
}

