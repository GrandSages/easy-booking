package com.easybooking.reservation.controller;

import com.easybooking.reservation.common.enums.ErrorEnums;
import com.easybooking.reservation.entity.LotteryOpenDateResult;
import com.easybooking.reservation.entity.Response;
import com.easybooking.reservation.service.crawler.HksixCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
public class CrawlerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerController.class);
    private final HksixCrawler crawler;

    public CrawlerController(HksixCrawler crawler) {
        this.crawler = crawler;
    }

    @GetMapping("/crawl")
    public Response crawl(HttpServletResponse response) {
        response.setStatus(200);
        LotteryOpenDateResult[] results;
        try {
            results = crawler.getRecordedCrawlResult();
        } catch (Exception e) {
            LOGGER.warn("Error at CrawlerController.crawl: ", e);
            return Response.fail(ErrorEnums.SERVER_ERROR);
        }
        return Response.ok(results);
    }
}
