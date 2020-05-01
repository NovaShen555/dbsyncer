package org.dbsyncer.web.controller;

import org.dbsyncer.biz.MappingService;
import org.dbsyncer.web.remote.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CyclicBarrier;

@Controller
@RequestMapping("/test")
public class TestController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private UserService userService;

    @Autowired
    private MappingService mappingService;

    @GetMapping("")
    public String index(HttpServletRequest request, ModelMap model) {
        return "test.html";
    }

    @RequestMapping("/demo")
    @ResponseBody
    public Object demo(Model model) {
        logger.info("demo");

        int size = 10;
        CyclicBarrier barrier = new CyclicBarrier(size);
        for (int i = 0; i < size; i++) {
            new Thread(()->{
                try {
                    logger.info("线程{}准备就绪", Thread.currentThread().getName());
                    barrier.await();
                    logger.info("线程{}执行中", Thread.currentThread().getName());
                    String start = mappingService.start("704107393226641408");
                    logger.info(start);
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            }).start();
        }


        return "hello";
    }

}