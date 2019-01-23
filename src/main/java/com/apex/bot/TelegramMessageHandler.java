/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 - 2019
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.apex.bot;

import com.apex.objects.Feedback;
import com.apex.strategy.CommandStrategy;
import com.apex.strategy.DeleteFileStrategy;
import com.apex.strategy.DeleteLinksStrategy;
import com.apex.strategy.IStrategy;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class TelegramMessageHandler extends ATelegramBot {

    private IStrategy deleteFile = new DeleteFileStrategy();
    private IStrategy deleteLinks = new DeleteLinksStrategy();
    private IStrategy runCommand = new CommandStrategy();
    private static final List<Integer> WHITELIST = Arrays.asList(512328408, 521684737, 533756221, 331773699, 516271269, 497516201, 454184647);
    private static final List<Long> CHAT = Arrays.asList(-1001385910531L, -1001175224299L, -1001417745659L);
    public static final long VERIFICATON = -1001417745659L;

    TelegramMessageHandler(String token, String botname) {
        super(token, botname);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onUpdateReceived(Update update) {

        try {

            if(update.hasCallbackQuery()){
                try {
                    CallbackQuery query = update.getCallbackQuery();
                    String callbackData = query.getData();
                    if(callbackData != null && !callbackData.equals("false")){

                        String [] arg = callbackData.split(",");

                        DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                        ConcurrentMap map = database.hashMap("feedback").createOrOpen();
                        Feedback feedback = (Feedback) map.get(arg[1]);
                        database.close();

                        if(arg[0].equals("blacklist")) {
                            database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                            ConcurrentMap mapUrlBlackList = database.hashMap("urlBlackList").createOrOpen();
                            mapUrlBlackList.put(feedback.getDataToBan(), feedback.getUserId());
                            database.close();
                        }

                        KickChatMember ban = new KickChatMember();
                        ban.setUserId(feedback.getUserId());
                        ban.setChatId(feedback.getChatId());
                        ban.setUntilDate(new BigDecimal(Instant.now().getEpochSecond()).intValue());
                        execute(ban);
                    }

                    DeleteMessage deleteMessage = new DeleteMessage(VERIFICATON,  query.getMessage().getMessageId());
                    execute(deleteMessage);
                } catch (Exception e){
                    log.error("Error in Callback");
                    log.error(e.getMessage());
                }
            }

            if (CHAT.contains(update.getMessage().getChatId())) {
                if (update.getMessage().getNewChatMembers() != null) {
                    DB database = DBMaker.fileDB("file.db").checksumHeaderBypass().make();
                    ConcurrentMap userMap = database.hashMap("user").createOrOpen();
                    for (User user : update.getMessage().getNewChatMembers()) {
                        userMap.put(user.getId(), Instant.now().getEpochSecond());
                    }
                    database.close();
                    log.info("Added User");
                }

                int from = update.getMessage().getFrom().getId();
                ArrayList<Optional<BotApiMethod>> commands;

                if(update.hasMessage()) {
                    if (WHITELIST.contains(from)) {
                        commands = runCommand.runStrategy(update);
                        for (Optional<BotApiMethod> method : commands) {
                            method.ifPresent(command -> {
                                try {
                                    execute(command);
                                    log.info("Command fired");
                                } catch (TelegramApiException e) {
                                    log.error("Failed execute Command" + e.getMessage());
                                }
                            });
                        }
                    }
                }

                if (update.getMessage().hasDocument()) {
                    commands = deleteFile.runStrategy(update);
                    for(Optional<BotApiMethod> method : commands) {
                        method.ifPresent(delete -> {
                            try {
                                execute(delete);
                                log.info("Deleted File");
                            } catch (TelegramApiException e) {
                                log.error("Failed to delete File" + e.getMessage());
                            }
                        });
                    }
                }

                commands = deleteLinks.runStrategy(update);
                for(Optional<BotApiMethod> method : commands) {
                method.ifPresent(delete -> {
                        try {
                            execute(delete);
                            log.info("Deleted Link");
                        } catch (TelegramApiException e) {
                            log.error("Failed to delete Link" + e.getMessage());
                        }
                    });
                }
            }

        } catch (Exception e) {}
    }
}
