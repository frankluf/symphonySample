# $Id$
######################################################################
#
# 		Makefile for Java Samples
#  ------- R E A D   M E   T O   A V O I D    F A I L U R E  --------
#
#
#
######################################################################

THIRD_LIB=lib
BIN=bin
SRC=src/com/platform/symphony/samples/CloudProxyClient
SERVICE_JAR=CloudProxyClient.jar

## building process

build all: CloudProxyClientjava jar

makedir:
	if [ ! -d ${BIN} ]; then \
		mkdir -p ${BIN} ; \
	fi

CloudProxyClientjava: makedir
	javac -s "${SRC}" -d "${BIN}" -classpath "${THIRD_LIB}/*" ${SRC}/api/*.java ${SRC}/rest/*.java ${SRC}/constant/*.java ${SRC}/util/*.java ${SRC}/model/*.java ${SRC}/loadbalance/*.java ${SRC}/*.java
	if [ ! -d ${BIN}/com/platform/symphony/samples/CloudProxyClient/msg ]; then \
		mkdir ${BIN}/com/platform/symphony/samples/CloudProxyClient/msg ; \
	fi
	cp ${SRC}/msg/* ${BIN}/com/platform/symphony/samples/CloudProxyClient/msg 

jar:
	cd ${BIN}; \
	jar -xf "../${THIRD_LIB}/commons-beanutils-1.8.3.jar"; \
	jar -xf "../${THIRD_LIB}/commons-codec-1.8.jar"; \
	jar -xf "../${THIRD_LIB}/commons-collections-3.2.1.jar"; \
	jar -xf "../${THIRD_LIB}/commons-lang3-3.4.jar"; \
	jar -xf "../${THIRD_LIB}/commons-lang-2.6.jar"; \
	jar -xf "../${THIRD_LIB}/commons-logging-1.1.1.jar"; \
	jar -xf "../${THIRD_LIB}/commons-cli-1.2.jar"; \
	jar -xf "../${THIRD_LIB}/converter-jackson-2.1.0.jar"; \
	jar -xf "../${THIRD_LIB}/guava-20.0.jar"; \
	jar -xf "../${THIRD_LIB}/jackson-annotations-2.9.9.jar"; \
	jar -xf "../${THIRD_LIB}/jackson-core-2.9.9.jar"; \
	jar -xf "../${THIRD_LIB}/jackson-databind-2.9.9.2.jar"; \
	jar -xf "../${THIRD_LIB}/jackson-datatype-joda-2.7.2.jar"; \
	jar -xf "../${THIRD_LIB}/log4j-1.2.16.jar"; \
	jar -xf "../${THIRD_LIB}/logging-interceptor-3.3.1.jar"; \
	jar -xf "../${THIRD_LIB}/slf4j-api-1.7.22.jar"; \
	jar -xf "../${THIRD_LIB}/httpclient-4.5.jar"; \
	jar -xf "../${THIRD_LIB}/httpcore-4.4.1.jar"; \
	jar -xf "../${THIRD_LIB}/httpmime-4.5.jar"; \
	jar -cmf ../MANIFEST.MF ${SERVICE_JAR} .; \
	cd ..; \
	cp ${BIN}/${SERVICE_JAR} .; \
	rm -rf ${BIN}
	
clean:
	rm -rf ${BIN}
	rm -rf ${SERVICE_JAR}
