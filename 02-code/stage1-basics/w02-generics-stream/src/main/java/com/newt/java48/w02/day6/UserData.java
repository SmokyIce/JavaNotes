package com.newt.java48.w02.day6;

import java.util.List;

/** Day 6 三个入口共用的一份数据：15 条，含 1 条 name=null、2 条 age=null、1 条 dept=null。 */
final class UserData {

    private UserData() {
    }

    static List<User> users() {
        return List.of(
                new User("U01", "张伟", "研发", 28, 22000, true, "北京"),
                new User("U02", "李娜", "研发", 34, 31000, true, "北京"),
                new User("U03", "王强", "研发", 25, 18000, false, "上海"),
                new User("U04", "周涛", "研发", 41, 45000, true, "深圳"),
                new User("U05", "何静", "研发", null, 22000, true, "北京"),   // age = null，且与张伟同薪（用来验年龄这一级）
                new User("U06", "刘洋", "产品", 31, 26000, true, "上海"),
                new User("U07", "陈静", "产品", 29, 24000, true, "北京"),
                new User("U08", "郑凯", "产品", 36, 33000, true, "上海"),
                new User("U09", "杨帆", "运营", 26, 15000, false, "广州"),
                new User("U10", "赵敏", "运营", 33, 19000, true, "广州"),
                new User("U11", "孙浩", "运营", 38, 21000, true, "深圳"),
                new User("U12", "吴磊", "测试", 27, 16000, true, "北京"),
                new User("U13", "徐丽", "测试", null, 17000, false, "上海"),  // age = null
                new User("U14", null, "测试", 30, 15500, true, "北京"),        // name = null
                new User("U99", "钱多", null, 45, 50000, true, "北京"));      // dept = null
    }
}
