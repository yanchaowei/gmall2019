package com.atguigu.gmall.payment.testMq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class BossTopic {
    public static void main(String[] args) {

        ConnectionFactory connect = new ActiveMQConnectionFactory("tcp://localhost:61616");
        try {
            //建立连接
            Connection connection = connect.createConnection();
            connection.start();

            //创建会话
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            //消息的队列模式
//            Queue testqueue = session.createQueue("TEST1");

            //消息的话题模式
            //话题模式的消息默认不持久化，如果持久化也要在客户端进行持久化
            Topic testTopic = session.createTopic("TEST2");

            //消息的发送方
            MessageProducer producer = session.createProducer(testTopic);

            //设置消息内容
            TextMessage textMessage=new ActiveMQTextMessage();
            textMessage.setText("为闫山军区的伟大复兴而努力奋斗！");

            //持久性的
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(textMessage);

            //事务提交
            session.commit();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }

    }
}
