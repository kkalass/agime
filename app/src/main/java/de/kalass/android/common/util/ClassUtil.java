package de.kalass.android.common.util;

import com.google.common.base.Preconditions;
import com.google.common.reflect.Reflection;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * Created by klas on 05.03.14.
 */
public class ClassUtil {

    /**
     * Given a concrete class, return the type parameter of its specified superclass.
     *
     * Note: only works for Class parameters, not for generic ones.
     * <code>
     *
     * BaseClass<T> {}
     *
     * TestClass extends BaseClass<String> {}
     *
     * getParameterClass(TestClass.class, BaseClass.class).equals(String.class)
     * </code>
     *
     * @return the java class of the type parameter
     */
    public static <B, T extends B> Class<?> getParameterClass(Class<T> cls, Class<B> baseClass) {


        Preconditions.checkArgument(!baseClass.isInterface());
        Preconditions.checkArgument(baseClass.getTypeParameters().length == 1, "Base Class must have exactly one type parameter");
        TypeToken<T> typeToken = TypeToken.of(cls);
        TypeToken<? super T> superType = typeToken.getSupertype(baseClass);
        /*
        Class<?> superclass = cls == null ? null : cls.getSuperclass();
        if (superclass == null) {
            throw new IllegalStateException("" + baseClass + " is not a superclass of " + cls );
        }
        if (!superclass.equals(baseClass)) {
            return getParameterClass(superclass, baseClass);
        }
        */
        if (superType == null) {
            throw new IllegalStateException("" + baseClass + " is not a superclass of " + cls );
        }
        ParameterizedType genericSuperclass = (ParameterizedType) superType.getType();
        Type firstType = genericSuperclass.getActualTypeArguments()[0];
        if (firstType instanceof GenericArrayType && ((GenericArrayType)firstType).getGenericComponentType() instanceof Class) {
            GenericArrayType arrayType = (GenericArrayType) firstType;
            // FIXME honestly? Is this the only way to return the correct class if the parameter
            //       was for example "byte[]" ?
            Class<?> t = (Class<?>)arrayType.getGenericComponentType();
            return Array.newInstance(t, 0).getClass();
        }

        return (Class<?>) firstType;
    }
}
