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

import com.apex.ATelegramBot;
import com.apex.addition.FeedbackAction;
import com.apex.entities.Blacklist;
import com.apex.entities.Feedback;
import com.apex.entities.TGUser;
import com.apex.repository.IBlackListRepository;
import com.apex.repository.IFeedbackRepository;
import com.apex.repository.ITGUserRepository;
import com.apex.strategy.CommandStrategy;
import com.apex.strategy.DeleteFileStrategy;
import com.apex.strategy.DeleteLinksStrategy;
import com.apex.strategy.IStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class TelegramMessageHandler extends ATelegramBot {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Value("${bot.verification}")
    private long verification;

    @Value("${bot.whitelist}")
    private List<Integer> whitelist;

    @Value("${bot.chat}")
    private List<Long> chat;

    @Autowired
    private IFeedbackRepository feedbackRepository;

    @Autowired
    private ITGUserRepository tgUserRepository;

    @Autowired
    private IBlackListRepository blackListRepository;

    @Autowired
    private DeleteFileStrategy deleteFile;

    @Autowired
    private DeleteLinksStrategy deleteLinks;

    @Autowired
    private CommandStrategy runCommand;

    @Autowired
    public TelegramMessageHandler(@Value("${bot.token}") String botToken, @Value("${bot.name}") String botName) {
        super(botToken, botName);
    }

    @Override
    public void onUpdateReceived(Update update) {

        try {

            if (update.hasCallbackQuery()) {
                try {
                    final CallbackQuery query = update.getCallbackQuery();
                    final String callbackData = query.getData();
                    if (callbackData != null && !callbackData.equals(FeedbackAction.IGNORE.getAction())) {

                        final String[] arg = callbackData.split(",");
                        final String action = arg[0];
                        final String feedbackId = arg[1];
                        final Optional<Feedback> feedbackOpt = feedbackRepository.findById(feedbackId);
                        feedbackOpt.ifPresent(feedback -> {
                            if (action.equals(FeedbackAction.BAN.getAction())) {
                                if (!feedback.getData().equals(""))
                                    blackListRepository.save(new Blacklist(feedback.getData()));
                                try {
                                    KickChatMember ban = new KickChatMember();
                                    ban.setUserId(feedback.getUserId());
                                    ban.setChatId(feedback.getChatId());
                                    ban.setUntilDate(new BigDecimal(Instant.now().getEpochSecond()).intValue());
                                    execute(ban);
                                } catch (Exception e) {
                                    log.info("Cant ban User!");
                                }
                            } else if (action.equals(FeedbackAction.WHITELIST.getAction())) {
                                tgUserRepository.save(new TGUser(feedback.getUserId(), 0, true));
                            }
                        });
                    }
                    execute(new DeleteMessage(verification, query.getMessage().getMessageId()));
                } catch (Exception e) {
                    log.error("Error in Callback");
                    log.error(e.getMessage());
                }
            }

            if (update.getMessage() != null) {

                final long chatId = update.getMessage().getChatId();
                final int fromUser = update.getMessage().getFrom().getId();

                if (chat.contains(chatId)) {

                    final ArrayList<BotApiMethod> commands = new ArrayList<>();

                    if (update.hasMessage()) {
                        if (whitelist.contains(fromUser)) {
                            commands.addAll(runCommand.runStrategy(update));
                        }
                    }

                    if (whitelist.contains(fromUser)) {
                        if (update.getMessage().hasDocument()) {
                            commands.addAll(deleteFile.runStrategy(update));
                        }
                        commands.addAll(deleteLinks.runStrategy(update));
                    }

                    commands.forEach(command -> {
                        try {
                            execute(command);
                        } catch (TelegramApiException e) {
                            log.error("Failed to execute Command with " + e.getMessage());
                        }
                    });
                }

            }

        } catch (Exception e) {
            log.info("Got an unknown message. Ignore");
        }
    }
}
