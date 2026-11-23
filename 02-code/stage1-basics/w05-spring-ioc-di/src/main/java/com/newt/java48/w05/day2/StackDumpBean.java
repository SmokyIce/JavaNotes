package com.newt.java48.w05.day2;

import com.newt.java48.w05.support.Stacks;

import javax.annotation.PostConstruct;

/**
 * 在 {@code @PostConstruct} 里把栈打出来 —— 回答「这个回调到底是谁调的」。
 *
 * <p>不用猜、不用背：栈上会明明白白出现
 * {@code InitDestroyAnnotationBeanPostProcessor.postProcessBeforeInitialization}，
 * 再往上是 {@code applyBeanPostProcessorsBeforeInitialization} 和 {@code initializeBean}。
 * 也就是说 {@code @PostConstruct} 的执行者是<b>后处理器</b>（具体是
 * {@code CommonAnnotationBeanPostProcessor}），不是容器的主流程。
 */
public class StackDumpBean {

    @PostConstruct
    public void init() {
        System.out.println("  ── @PostConstruct 的执行栈（只留 spring 与 com.newt 的帧）──");
        Stacks.springFrames("@PostConstruct 被调用的那一刻：", 8);
    }
}
