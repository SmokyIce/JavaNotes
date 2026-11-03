package com.newt.java48.w02.day6;

/**
 * Day 6 的统一模型。
 *
 * <p>故意留了三个 null，它们分别对应三个坑：
 * <ul>
 *   <li>{@code name} 有一条为 null（历史脏数据）—— A 段「过滤时 NPE」</li>
 *   <li>{@code age} 有两条为 null（没登记）—— B 段「排序时 NPE」/ C 段「averagingInt 拆箱 NPE」</li>
 *   <li>{@code dept} 有一条为 null —— D 段「groupingBy 的 key 为 null」</li>
 * </ul>
 * 真实业务里这三个 null 通常不在同一份数据上，但对于只需要跑一遍的实验，凑一起更省事。
 */
record User(String id, String name, String dept, Integer age, int salary, boolean active, String city) {

    /** 排序/分组结果里用短格式，一行能放下好几个人。 */
    String brief() {
        return (name == null ? "<null>" : name) + "(" + salary + ")";
    }

    @Override
    public String toString() {
        return id + " " + brief() + " " + dept + " " + age + " " + city + (active ? " 在职" : " 离职");
    }
}
