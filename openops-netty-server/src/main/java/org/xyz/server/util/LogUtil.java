package org.xyz.server.util;

import io.netty.channel.Channel;
import io.netty.util.Attribute;
import org.xyz.server.constants.AttrConstants;

public class LogUtil {
    public static void markAsLogin(Channel channel) {
        channel.attr(AttrConstants.login).set(true);
    }

    public static boolean hasLogin(Channel channel) {
        Attribute<Boolean> loginAttr  = channel.attr(AttrConstants.login);
        return loginAttr.get() != null;
    }
}
