package me.timur.taxibekatbot.util

import java.beans.IntrospectionException
import java.beans.PropertyDescriptor
import java.lang.reflect.InvocationTargetException


class InvokeGetter {

    companion object{
        fun invokeGetter(obj: Any, variableName: String?): Any? {
            return try {
                val pd = PropertyDescriptor(variableName, obj.javaClass)
                val getter = pd.readMethod
                getter.invoke(obj)
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
                null
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                null
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
                null
            } catch (e: IntrospectionException) {
                e.printStackTrace()
                null
            }
        }
    }

}