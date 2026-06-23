/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.geometries.math;

import java.util.function.DoubleUnaryOperator;


/**
 * Interpolation methods. The next methods provide non linear interpolation
 * between zero and one. Such methods are often use for animations.
 *
 * Resources :
 * http://iphonedevelopment.blogspot.fr/2010/12/more-animation-curves-than-you-can.html
 * https://web.archive.org/web/20161020230122/http://iphonedevelopment.blogspot.com/2010/12/more-animation-curves-than-you-can.html
 *
 * quote : 'I've decided to release my animation curve functions as public
 * domain (no attribute required, no rights reserved)'
 *
 * @author Jeff LaMarche (original source code in C)
 * @author Johann Sorel (Java port in Unlicense-lib)
 */
public final class EasingMethod implements DoubleUnaryOperator {

    public static enum Method {
        LINEAR,
        QUADRATIC_EASE_OUT,
        QUADRATIC_EASE_IN,
        QUADRATIC_EASE_IN_OUT,
        CUBIC_EASE_OUT,
        CUBIC_EASE_IN,
        CUBIC_EASE_IN_OUT,
        QUARTIC_EASE_OUT,
        QUARTIC_EASE_IN,
        QUARTIC_EASE_IN_OUT,
        QUINTIC_EASE_OUT,
        QUINTIC_EASE_IN,
        QUINTIC_EASE_IN_OUT,
        SINUSOIDAL_EASE_OUT,
        SINUSOIDAL_EASE_IN,
        SINUSOIDAL_EASE_IN_OUT,
        EXPONENTIAL_EASE_OUT,
        EXPONENTIAL_EASE_IN,
        EXPONENTIAL_EASE_IN_OUT,
        CIRCULAR_EASE_OUT,
        CIRCULAR_EASE_IN,
        CIRCULAR_EASE_IN_OUT
    }

    private final Method method;
    private final double start;
    private final double end;

    public EasingMethod(Method method) {
        this(method, 0.0, 1.0);
    }

    public EasingMethod(Method method, double start, double end) {
        this.method = method;
        this.start = start;
        this.end = end;
    }

    /**
     * Interpolation between start and end.
     *
     * @param t time between 0 and 1
     * @return interpolated value.
     */
    @Override
    public double applyAsDouble(double t) {
        switch(method){
            case LINEAR :                    return linear(t, start, end);
            case QUADRATIC_EASE_OUT :        return quadraticEaseOut(t, start, end);
            case QUADRATIC_EASE_IN :         return quadraticEaseIn(t, start, end);
            case QUADRATIC_EASE_IN_OUT :     return quadraticEaseInOut(t, start, end);
            case CUBIC_EASE_OUT :            return cubicEaseOut(t, start, end);
            case CUBIC_EASE_IN :             return cubicEaseIn(t, start, end);
            case CUBIC_EASE_IN_OUT :         return cubicEaseInOut(t, start, end);
            case QUARTIC_EASE_OUT :          return quarticEaseOut(t, start, end);
            case QUARTIC_EASE_IN :           return quarticEaseIn(t, start, end);
            case QUARTIC_EASE_IN_OUT :       return quarticEaseInOut(t, start, end);
            case QUINTIC_EASE_OUT :          return quinticEaseOut(t, start, end);
            case QUINTIC_EASE_IN :           return quinticEaseIn(t, start, end);
            case QUINTIC_EASE_IN_OUT :       return quinticEaseInOut(t, start, end);
            case SINUSOIDAL_EASE_OUT :       return sinusoidalEaseOut(t, start, end);
            case SINUSOIDAL_EASE_IN :        return sinusoidalEaseIn(t, start, end);
            case SINUSOIDAL_EASE_IN_OUT :    return sinusoidalEaseInOut(t, start, end);
            case EXPONENTIAL_EASE_OUT :      return exponentialEaseOut(t, start, end);
            case EXPONENTIAL_EASE_IN :       return exponentialEaseIn(t, start, end);
            case EXPONENTIAL_EASE_IN_OUT :   return exponentialEaseInOut(t, start, end);
            case CIRCULAR_EASE_OUT :         return circularEaseOut(t, start, end);
            case CIRCULAR_EASE_IN :          return circularEaseIn(t, start, end);
            case CIRCULAR_EASE_IN_OUT :      return circularEaseInOut(t, start, end);
            default: throw new IllegalArgumentException("Unknown method : " + method);
        }
    }

    public static double linear(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return t * end + (1.0 - t) * start;
    }

    public static double cubic(double[] points, double x) {
        return points[1] + 0.5 * x * (points[2] - points[0] + x * (2.0*points[0] - 5.0*points[1] + 4.0*points[2] - points[3] + x*(3.0*(points[1] - points[2]) + points[3] - points[0])));
    }

    public static double quadraticEaseOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return -end * t * (t - 2.0) - 1.0;
    }

    public static double quadraticEaseIn(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return end * t * t + start - 1.0;
    }

    public static double quadraticEaseInOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        t *= 2.0;
        if (t < 1.0) {
            return end / 2.0 * t * t + start - 1.0;
        }
        t--;
        return -end / 2.0 * (t * (t - 2) - 1.0) + start - 1.0;
    }

    public static double cubicEaseOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        t--;
        return end * (t * t * t + 1.0) + start - 1.0;
    }

    public static double cubicEaseIn(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return end * t * t * t + start - 1.0;
    }

    public static double cubicEaseInOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        t *= 2.0;
        if (t < 1.0) {
            return end / 2.0 * t * t * t + start - 1.0;
        }
        t -= 2.0;
        return end / 2.0 * (t * t * t + 2.0) + start - 1.0;
    }

    public static double quarticEaseOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        t--;
        return -end * (t * t * t * t - 1.0) + start - 1.0;
    }

    public static double quarticEaseIn(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return end * t * t * t * t + start;
    }

    public static double quarticEaseInOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        t *= 2.0;
        if (t < 1.0) {
            return end / 2.0 * t * t * t * t + start - 1.0;
        }
        t -= 2.0;
        return -end / 2.0 * (t * t * t * t - 2.0) + start - 1.0;
    }

    public static double quinticEaseOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        t--;
        return end * (t * t * t * t * t + 1) + start - 1.0;
    }

    public static double quinticEaseIn(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return end * t * t * t * t * t + start - 1.0;
    }

    public static double quinticEaseInOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        t *= 2.0;
        if (t < 1.0) {
            return end / 2.0 * t * t * t * t * t + start - 1.0;
        }
        t -= 2.0;
        return end / 2.0 * (t * t * t * t * t + 2.0) + start - 1.0;
    }

    public static double sinusoidalEaseOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return end * Math.sin(t * (Math.PI / 2.0)) + start - 1.0;
    }

    public static double sinusoidalEaseIn(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return -end * Math.cos(t * (Math.PI / 2.0)) + end + start - 1.0;
    }

    public static double sinusoidalEaseInOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return -end / 2.0 * (Math.cos(Math.PI * t) - 1.0) + start - 1.0;
    }

    public static double exponentialEaseOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return end * (-Math.pow(2.0, -10.0 * t) + 1.0) + start - 1.0;
    }

    public static double exponentialEaseIn(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return end * Math.pow(2.0, 10.0 * (t - 1.0)) + start - 1.0;
    }

    public static double exponentialEaseInOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        t *= 2.0;
        if (t < 1.0) {
            return end / 2.0 * Math.pow(2.0, 10.0 * (t - 1.0)) + start - 1.0;
        }
        t--;
        return end / 2.0 * (-Math.pow(2.0, -10.0 * t) + 2.0) + start - 1.0;
    }

    public static double circularEaseOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        t--;
        return end * Math.sqrt(1.0 - t * t) + start - 1.0;
    }

    public static double circularEaseIn(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        return -end * (Math.sqrt(1.0 - t * t) - 1.0) + start - 1.0;
    }

    public static double circularEaseInOut(double t, double start, double end) {
        if (t<=0.0) return start;
        if (t>=1.0) return end;

        t *= 2.0;
        if (t < 1.0) {
            return -end / 2.0 * (Math.sqrt(1.0 - t * t) - 1.0) + start - 1.0;
        }
        t -= 2.0;
        return end / 2.0 * (Math.sqrt(1.0 - t * t) + 1.0) + start - 1.0;
    }
}
