package de.greensurvivors.eventhelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;

public class FinalFieldUtil {

    private static class MemberNameWrapper {

        protected final Class<?> MEMBER_NAME_CLASS;
        private final Constructor<?> MEMBER_NAME_CONSTRUCTOR;
        private final Field MEMBER_NAME_FLAGS_FIELD;
        private final Method MEMBER_NAME_GET_REFERENCE_KIND_METHOD;

        {
            try {
                MEMBER_NAME_CLASS = Class.forName("java.lang.invoke.MemberName");

                MEMBER_NAME_CONSTRUCTOR = MEMBER_NAME_CLASS.getDeclaredConstructor(Field.class, boolean.class); //e.g. new MemberName(myField, true);
                MEMBER_NAME_CONSTRUCTOR.setAccessible(true);

                MEMBER_NAME_FLAGS_FIELD = MEMBER_NAME_CLASS.getDeclaredField("flags");
                MEMBER_NAME_FLAGS_FIELD.setAccessible(true);

                MEMBER_NAME_GET_REFERENCE_KIND_METHOD = MEMBER_NAME_CLASS.getDeclaredMethod("getReferenceKind");
                MEMBER_NAME_GET_REFERENCE_KIND_METHOD.setAccessible(true);
            } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        final Object instance;

        public MemberNameWrapper(Field field, boolean makeSetter) {
            try {
                instance = MEMBER_NAME_CONSTRUCTOR.newInstance(field, makeSetter);
                removeFinality(instance);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void removeFinality(Object memberNameInstance) throws IllegalAccessException {
            //Manipulate flags to remove hints to it being final
            final int initialFlags = MEMBER_NAME_FLAGS_FIELD.getInt(memberNameInstance);

            if (!Modifier.isFinal(initialFlags)) {
                return;
            }

            final int nonFinalFlags = initialFlags & ~Modifier.FINAL;

            MEMBER_NAME_FLAGS_FIELD.setInt(memberNameInstance, nonFinalFlags);
        }

        protected Object getMemberNameInstance() {
            return instance;
        }

        protected byte getReferenceKind() {
            try {
                return (byte) MEMBER_NAME_GET_REFERENCE_KIND_METHOD.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static class LookupWrapper {

        private final Class<?> LOOKUP_CLASS;
        private final Method LOOKUP_GET_FIELD_VAR_HANDLE_NO_SECURITY_MANAGER_METHOD;

        {
            try {
                LOOKUP_CLASS = MethodHandles.Lookup.class;
                LOOKUP_GET_FIELD_VAR_HANDLE_NO_SECURITY_MANAGER_METHOD = LOOKUP_CLASS.getDeclaredMethod("getDirectFieldNoSecurityManager", byte.class, Class.class, Class.forName("java.lang.invoke.MemberName"));
                LOOKUP_GET_FIELD_VAR_HANDLE_NO_SECURITY_MANAGER_METHOD.setAccessible(true);

            } catch (NoSuchMethodException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        private final MethodHandles.Lookup lookup;

        private LookupWrapper(MethodHandles.Lookup lookup) {
            this.lookup = lookup;
        }

        public MethodHandle unreflectVarHandleUnrestrictedSetter(Field field) {
            final MemberNameWrapper memberNameSetterWrapper = new MemberNameWrapper(field, true);
            final byte setterReferenceKind = memberNameSetterWrapper.getReferenceKind();
            final Object memberNameSetter = memberNameSetterWrapper.getMemberNameInstance();


            try {
                return (MethodHandle) LOOKUP_GET_FIELD_VAR_HANDLE_NO_SECURITY_MANAGER_METHOD.invoke(lookup, setterReferenceKind, field.getDeclaringClass(), memberNameSetter);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public MethodHandle unreflectVarHandleUnrestrictedGetter(Field field) {
            final MemberNameWrapper memberNameSetterWrapper = new MemberNameWrapper(field, false);
            final byte setterReferenceKind = memberNameSetterWrapper.getReferenceKind();
            final Object memberNameSetter = memberNameSetterWrapper.getMemberNameInstance();


            try {
                return (MethodHandle) LOOKUP_GET_FIELD_VAR_HANDLE_NO_SECURITY_MANAGER_METHOD.invoke(lookup, setterReferenceKind, field.getDeclaringClass(), memberNameSetter);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void setStaticFinalField(Field field, Object value) throws Throwable {
        if (Modifier.isFinal(field.getModifiers()) && field.getType().isPrimitive()) {
            throw new IllegalArgumentException("primitive finals are not supported, because their modification depends on very specific circumstances.");
        }

        final LookupWrapper lookupWrapper = new LookupWrapper(MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup()));
        final MethodHandle methodHandle = lookupWrapper.unreflectVarHandleUnrestrictedSetter(field);
        methodHandle.invoke(value);
    }

    public static Object getStaticFinalField(Field field) throws Throwable {
        if (Modifier.isFinal(field.getModifiers()) && field.getType().isPrimitive()) {
            throw new IllegalArgumentException("primitive finals are not supported, because their modification depends on very specific circumstances.");
        }

        final LookupWrapper lookupWrapper = new LookupWrapper(MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup()));
        final MethodHandle methodHandle = lookupWrapper.unreflectVarHandleUnrestrictedGetter(field);
        return methodHandle.invoke();
    }
}
