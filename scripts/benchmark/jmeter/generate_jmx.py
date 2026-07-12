#!/usr/bin/env python3
"""Generate JMeter .jmx test plans for online-shop benchmark."""

import argparse
import os
import xml.etree.ElementTree as ET
from xml.dom import minidom


def string_prop(name: str, value: str) -> ET.Element:
    """Create a JMeter stringProp element."""
    elem = ET.Element("stringProp")
    elem.set("name", name)
    elem.text = value
    return elem


def bool_prop(name: str, value: bool) -> ET.Element:
    """Create a JMeter boolProp element."""
    elem = ET.Element("boolProp")
    elem.set("name", name)
    elem.text = "true" if value else "false"
    return elem


def int_prop(name: str, value: int) -> ET.Element:
    """Create a JMeter intProp element."""
    elem = ET.Element("intProp")
    elem.set("name", name)
    elem.text = str(value)
    return elem


def long_prop(name: str, value: int) -> ET.Element:
    """Create a JMeter longProp element."""
    elem = ET.Element("longProp")
    elem.set("name", name)
    elem.text = str(value)
    return elem


def element_prop(name: str, element_type: str, **attrs) -> ET.Element:
    """Create a JMeter elementProp element."""
    elem = ET.Element("elementProp")
    elem.set("name", name)
    elem.set("elementType", element_type)
    for k, v in attrs.items():
        elem.set(k, v)
    return elem


def collection_prop(name: str) -> ET.Element:
    """Create a JMeter collectionProp element."""
    elem = ET.Element("collectionProp")
    elem.set("name", name)
    return elem


def argument(name: str, value: str, desc: str = "") -> ET.Element:
    """Create a user-defined variable argument."""
    prop = element_prop(name, "Argument", metadata="=")
    prop.append(string_prop("Argument.name", name))
    prop.append(string_prop("Argument.value", value))
    if desc:
        prop.append(string_prop("Argument.desc", desc))
    prop.append(string_prop("Argument.metadata", "="))
    return prop


def test_plan(name: str) -> tuple[ET.Element, ET.Element]:
    """Create a JMeter TestPlan and return (plan, root_hash_tree)."""
    root = ET.Element("jmeterTestPlan")
    root.set("version", "1.2")
    root.set("properties", "5.0")
    root.set("jmeter", "5.4.1")

    root_hash = ET.SubElement(root, "hashTree")
    plan = ET.SubElement(root_hash, "TestPlan")
    plan.set("guiclass", "TestPlanGui")
    plan.set("testclass", "TestPlan")
    plan.set("testname", name)
    plan.set("enabled", "true")
    plan.append(string_prop("TestPlan.comments", ""))
    plan.append(bool_prop("TestPlan.functional_mode", False))
    plan.append(bool_prop("TestPlan.tearDown_on_shutdown", True))
    plan.append(bool_prop("TestPlan.serialize_threadgroups", False))

    udv = element_prop("TestPlan.user_defined_variables", "Arguments",
                       guiclass="ArgumentsPanel", testclass="Arguments",
                       testname="User Defined Variables", enabled="true")
    udv.append(collection_prop("Arguments.arguments"))
    plan.append(udv)
    plan.append(string_prop("TestPlan.user_define_classpath", ""))

    plan_hash = ET.SubElement(root_hash, "hashTree")
    return root, plan_hash


def user_defined_variables(plan_hash: ET.Element, variables: dict[str, str]) -> None:
    """Add user defined variables to the test plan."""
    udv = ET.SubElement(plan_hash, "Arguments")
    udv.set("guiclass", "ArgumentsPanel")
    udv.set("testclass", "Arguments")
    udv.set("testname", "User Defined Variables")
    udv.set("enabled", "true")
    coll = collection_prop("Arguments.arguments")
    for name, value in variables.items():
        coll.append(argument(name, value))
    udv.append(coll)
    ET.SubElement(plan_hash, "hashTree")


def thread_group(plan_hash: ET.Element, name: str, num_threads: int, ramp_up: int,
                 loops: int, duration: int = 0) -> ET.Element:
    """Add a ThreadGroup and return its hashTree element."""
    tg = ET.SubElement(plan_hash, "ThreadGroup")
    tg.set("guiclass", "ThreadGroupGui")
    tg.set("testclass", "ThreadGroup")
    tg.set("testname", name)
    tg.set("enabled", "true")
    tg.append(string_prop("ThreadGroup.on_sample_error", "continue"))

    loop_controller = element_prop("ThreadGroup.main_controller", "LoopController",
                                   guiclass="LoopControlPanel", testclass="LoopController",
                                   testname="Loop Controller", enabled="true")
    loop_controller.append(bool_prop("LoopController.continue_forever", False))
    loop_controller.append(string_prop("LoopController.loops", "${__P(loops,%d)}" % loops))
    tg.append(loop_controller)

    tg.append(string_prop("ThreadGroup.num_threads", "${__P(threads,%d)}" % num_threads))
    tg.append(string_prop("ThreadGroup.ramp_time", "${__P(ramp_up,%d)}" % ramp_up))
    tg.append(bool_prop("ThreadGroup.scheduler", duration > 0))
    tg.append(string_prop("ThreadGroup.duration", str(duration)))
    tg.append(stringProp := string_prop("ThreadGroup.delay", "0"))
    tg.append(bool_prop("ThreadGroup.same_user_on_next_iteration", True))

    tg_hash = ET.SubElement(plan_hash, "hashTree")
    return tg_hash


def csv_data_set(tg_hash: ET.Element, filename: str, variable_names: str,
                 delimiter: str = ",") -> None:
    """Add a CSV Data Set Config."""
    csv = ET.SubElement(tg_hash, "CSVDataSet")
    csv.set("guiclass", "TestBeanGUI")
    csv.set("testclass", "CSVDataSet")
    csv.set("testname", "CSV Data Set Config")
    csv.set("enabled", "true")
    csv.append(string_prop("delimiter", delimiter))
    csv.append(string_prop("fileEncoding", "UTF-8"))
    csv.append(string_prop("filename", filename))
    csv.append(bool_prop("ignoreFirstLine", True))
    csv.append(bool_prop("quotedData", False))
    csv.append(bool_prop("recycle", True))
    csv.append(string_prop("shareMode", "shareMode.all"))
    csv.append(bool_prop("stopThread", False))
    csv.append(string_prop("variableNames", variable_names))
    ET.SubElement(tg_hash, "hashTree")


def http_sampler(tg_hash: ET.Element, name: str, domain: str, port: str, path: str,
                 method: str = "GET", protocol: str = "http", body: str = "",
                 content_type: str = "application/json") -> ET.Element:
    """Add an HTTP Request sampler and return its hashTree."""
    sampler = ET.SubElement(tg_hash, "HTTPSamplerProxy")
    sampler.set("guiclass", "HttpTestSampleGui")
    sampler.set("testclass", "HTTPSamplerProxy")
    sampler.set("testname", name)
    sampler.set("enabled", "true")
    sampler.append(bool_prop("HTTPSampler.postBodyRaw", bool(body)))

    if body:
        body_prop = element_prop("HTTPsampler.Arguments", "Arguments",
                                 guiclass="HTTPArgumentsPanel", testclass="Arguments",
                                 testname="", enabled="true")
        coll = collection_prop("Arguments.arguments")
        body_arg = element_prop("", "HTTPArgument", metadata="=")
        body_arg.append(bool_prop("HTTPArgument.always_encode", False))
        body_arg.append(string_prop("Argument.value", body))
        body_arg.append(string_prop("Argument.metadata", "="))
        body_arg.append(bool_prop("HTTPArgument.use_equals", True))
        body_arg.append(bool_prop("UseRandomString", False))
        coll.append(body_arg)
        body_prop.append(coll)
        sampler.append(body_prop)

    sampler.append(string_prop("HTTPSampler.domain", domain))
    sampler.append(string_prop("HTTPSampler.port", port))
    sampler.append(string_prop("HTTPSampler.protocol", protocol))
    sampler.append(string_prop("HTTPSampler.contentEncoding", "UTF-8"))
    sampler.append(string_prop("HTTPSampler.path", path))
    sampler.append(string_prop("HTTPSampler.method", method))
    sampler.append(bool_prop("HTTPSampler.follow_redirects", True))
    sampler.append(bool_prop("HTTPSampler.auto_redirects", False))
    sampler.append(bool_prop("HTTPSampler.use_keepalive", True))
    sampler.append(bool_prop("HTTPSampler.DO_MULTIPART_POST", False))
    sampler.append(string_prop("HTTPSampler.embedded_url_re", ""))
    sampler.append(string_prop("HTTPSampler.connect_timeout", "10000"))
    sampler.append(string_prop("HTTPSampler.response_timeout", "30000"))

    sampler_hash = ET.SubElement(tg_hash, "hashTree")

    # Add default header manager for content type if body present
    if body:
        header_mgr = ET.SubElement(sampler_hash, "HeaderManager")
        header_mgr.set("guiclass", "HeaderPanel")
        header_mgr.set("testclass", "HeaderManager")
        header_mgr.set("testname", "HTTP Headers")
        header_mgr.set("enabled", "true")
        coll = collection_prop("HeaderManager.headers")
        header = element_prop("", "Header", testname="", enabled="true")
        header.append(string_prop("Header.name", "Content-Type"))
        header.append(string_prop("Header.value", content_type))
        coll.append(header)
        header_mgr.append(coll)
        ET.SubElement(sampler_hash, "hashTree")

    return sampler_hash


def json_extractor(sampler_hash: ET.Element, name: str, json_path: str,
                   variable: str, match_no: str = "1", default: str = "NOT_FOUND") -> None:
    """Add a JSON PostProcessor to extract a value."""
    ext = ET.SubElement(sampler_hash, "JSONPostProcessor")
    ext.set("guiclass", "JSONPostProcessorGui")
    ext.set("testclass", "JSONPostProcessor")
    ext.set("testname", name)
    ext.set("enabled", "true")
    ext.append(string_prop("JSONPostProcessor.referenceNames", variable))
    ext.append(string_prop("JSONPostProcessor.jsonPathExprs", json_path))
    ext.append(string_prop("JSONPostProcessor.match_numbers", match_no))
    ext.append(string_prop("JSONPostProcessor.defaultValues", default))
    ET.SubElement(sampler_hash, "hashTree")


def response_assertion(sampler_hash: ET.Element, name: str, test_field: str,
                       pattern: str, contains: bool = True) -> None:
    """Add a Response Assertion."""
    assertion = ET.SubElement(sampler_hash, "ResponseAssertion")
    assertion.set("guiclass", "AssertionGui")
    assertion.set("testclass", "ResponseAssertion")
    assertion.set("testname", name)
    assertion.set("enabled", "true")
    assertion.append(collection_prop("Asserion.test_strings"))
    assertion[0].append(string_prop(str(len(assertion[0])), pattern))
    assertion.append(string_prop("Assertion.test_field", test_field))
    assertion.append(bool_prop("Assertion.assume_success", False))
    assertion.append(int_prop("Assertion.test_type", 2 if contains else 8))
    ET.SubElement(sampler_hash, "hashTree")


def json_assertion(sampler_hash: ET.Element, name: str, json_path: str,
                   expected_value: str) -> None:
    """Add a JSON Assertion that checks a JSON path equals expected value."""
    assertion = ET.SubElement(sampler_hash, "JSONAssertion")
    assertion.set("guiclass", "JSONAssertionGui")
    assertion.set("testclass", "JSONAssertion")
    assertion.set("testname", name)
    assertion.set("enabled", "true")
    assertion.append(string_prop("json_path", json_path))
    assertion.append(string_prop("expected_value", expected_value))
    assertion.append(bool_prop("json_validation", False))
    assertion.append(bool_prop("expect_null", False))
    assertion.append(bool_prop("invert", False))
    assertion.append(bool_prop("is_regex", False))
    ET.SubElement(sampler_hash, "hashTree")


def counter_config(tg_hash: ET.Element, name: str, var: str,
                   start: int, end: int, per_thread: bool = False) -> None:
    """Add a Counter config element that cycles through values."""
    counter = ET.SubElement(tg_hash, "CounterConfig")
    counter.set("guiclass", "CounterConfigGui")
    counter.set("testclass", "CounterConfig")
    counter.set("testname", name)
    counter.set("enabled", "true")
    counter.append(string_prop("CounterConfig.start", str(start)))
    counter.append(string_prop("CounterConfig.end", str(end)))
    counter.append(string_prop("CounterConfig.incr", "1"))
    counter.append(string_prop("CounterConfig.name", var))
    counter.append(string_prop("CounterConfig.format", ""))
    counter.append(bool_prop("CounterConfig.per_user", per_thread))
    counter.append(bool_prop("CounterConfig.reset_on_tg_iteration", True))
    ET.SubElement(tg_hash, "hashTree")


def result_collector(plan_hash: ET.Element, name: str, filename: str,
                     collector_class: str = "ViewResultsFullVisualizer") -> None:
    """Add a result collector (listener)."""
    rc = ET.SubElement(plan_hash, "ResultCollector")
    rc.set("guiclass", collector_class)
    rc.set("testclass", "ResultCollector")
    rc.set("testname", name)
    rc.set("enabled", "true")
    rc.append(bool_prop("ResultCollector.error_logging", False))

    obj_prop = element_prop("save_config", "SaveConfig",
                            guiclass="ResultCollectorSaveConfig",
                            testclass="SaveConfig", testname="", enabled="true")
    fields = [
        ("time", True), ("latency", True), ("timestamp", True), ("success", True),
        ("label", True), ("code", True), ("message", True), ("threadName", True),
        ("dataType", True), ("encoding", False), ("assertions", True), ("subresults", True),
        ("responseData", False), ("samplerData", False), ("xml", False), ("fieldNames", True),
        ("assertionsResultsToSave", 0), ("bytes", True), ("sentBytes", True),
        ("url", True), ("threadCounts", True), ("idleTime", True), ("connectTime", True),
    ]
    for field, value in fields:
        if isinstance(value, bool):
            obj_prop.append(bool_prop(f"saveConfig.{field}", value))
        else:
            obj_prop.append(int_prop(f"saveConfig.{field}", value))
    obj_prop.append(string_prop("saveConfig.filename", filename))
    obj_prop.append(bool_prop("saveConfig.hostname", False))
    obj_prop.append(bool_prop("saveConfig.responseHeaders", False))
    obj_prop.append(bool_prop("saveConfig.requestHeaders", False))
    obj_prop.append(bool_prop("saveConfig.responseDataOnError", False))
    obj_prop.append(string_prop("saveConfig.assertionsResultsToSave", "0"))
    obj_prop.append(int_prop("saveConfig.bytes", 1))
    rc.append(obj_prop)
    rc.append(string_prop("filename", filename))
    ET.SubElement(plan_hash, "hashTree")


def aggregate_report(plan_hash: ET.Element, filename: str) -> None:
    """Add an Aggregate Report listener writing to a CSV JTL file."""
    rc = ET.SubElement(plan_hash, "ResultCollector")
    rc.set("guiclass", "StatVisualizer")
    rc.set("testclass", "ResultCollector")
    rc.set("testname", "Aggregate Report")
    rc.set("enabled", "true")
    rc.append(bool_prop("ResultCollector.error_logging", False))

    obj_prop = element_prop("save_config", "SaveConfig",
                            guiclass="ResultCollectorSaveConfig",
                            testclass="SaveConfig", testname="", enabled="true")
    save_fields = [
        ("time", True), ("latency", True), ("timestamp", True), ("success", True),
        ("label", True), ("code", True), ("message", True), ("threadName", True),
        ("dataType", True), ("encoding", False), ("assertions", True), ("subresults", True),
        ("responseData", False), ("samplerData", False), ("xml", False), ("fieldNames", True),
        ("assertionsResultsToSave", 0), ("bytes", True), ("sentBytes", True),
        ("url", True), ("threadCounts", True), ("idleTime", True), ("connectTime", True),
    ]
    for field, value in save_fields:
        if isinstance(value, bool):
            obj_prop.append(bool_prop(f"saveConfig.{field}", value))
        else:
            obj_prop.append(int_prop(f"saveConfig.{field}", value))
    obj_prop.append(string_prop("saveConfig.filename", filename))
    obj_prop.append(bool_prop("saveConfig.hostname", False))
    obj_prop.append(bool_prop("saveConfig.responseHeaders", False))
    obj_prop.append(bool_prop("saveConfig.requestHeaders", False))
    obj_prop.append(bool_prop("saveConfig.responseDataOnError", False))
    obj_prop.append(string_prop("saveConfig.assertionsResultsToSave", "0"))
    obj_prop.append(int_prop("saveConfig.bytes", 1))
    rc.append(obj_prop)
    rc.append(string_prop("filename", filename))
    ET.SubElement(plan_hash, "hashTree")


def generate_product_detail_jmx(output_path: str, base_url: str = "localhost",
                                port: str = "8080", product_id: str = "13",
                                product_id_start: int = 1, product_id_end: int = 15,
                                threads: int = 100, loops: int = 30,
                                ramp_up: int = 5) -> None:
    """Generate a JMeter plan for product detail cache verification."""
    root, plan_hash = test_plan("Product Detail Cache Benchmark")
    user_defined_variables(plan_hash, {
        "base_url": base_url,
        "port": port,
    })

    tg_hash = thread_group(plan_hash, "Product Detail Threads", threads, ramp_up, loops)
    counter_config(tg_hash, "Product ID Counter", "product_id",
                   product_id_start, product_id_end, per_thread=False)
    sampler_hash = http_sampler(tg_hash, "GET /api/product/${product_id}",
                                "${base_url}", "${port}", "/api/product/${product_id}",
                                method="GET")
    response_assertion(sampler_hash, "Success Assertion", "Assertion.response_code",
                       "200")

    save_jmx(root, output_path)


def generate_order_create_jmx(output_path: str, csv_path: str, base_url: str = "localhost",
                              port: str = "8080", threads: int = 100, loops: int = 1,
                              ramp_up: int = 5) -> None:
    """Generate a JMeter plan for order creation benchmark."""
    root, plan_hash = test_plan("Order Create Benchmark")
    user_defined_variables(plan_hash, {
        "base_url": base_url,
        "port": port,
    })

    tg_hash = thread_group(plan_hash, "Order Create Threads", threads, ramp_up, loops)
    csv_data_set(tg_hash, csv_path, "username,addressId")

    # Login sampler
    login_hash = http_sampler(tg_hash, "POST /api/user/login",
                              "${base_url}", "${port}", "/api/user/login",
                              method="POST",
                              body='{"username":"${username}","password":"123456"}')
    response_assertion(login_hash, "Login Business Code 200",
                       "Assertion.response_data", '"code":200')
    json_extractor(login_hash, "Extract Token", "$.data", "token")

    # Create order sampler
    order_hash = http_sampler(tg_hash, "POST /api/order/create",
                              "${base_url}", "${port}", "/api/order/create",
                              method="POST",
                              body='{"addressId":${addressId},"remark":"jmeter-benchmark"}')
    # Add Authorization header dynamically
    header_mgr = ET.SubElement(order_hash, "HeaderManager")
    header_mgr.set("guiclass", "HeaderPanel")
    header_mgr.set("testclass", "HeaderManager")
    header_mgr.set("testname", "Auth Header")
    header_mgr.set("enabled", "true")
    coll = collection_prop("HeaderManager.headers")
    header = element_prop("", "Header", testname="", enabled="true")
    header.append(string_prop("Header.name", "Authorization"))
    header.append(string_prop("Header.value", "${token}"))
    coll.append(header)
    header_mgr.append(coll)
    ET.SubElement(order_hash, "hashTree")

    json_extractor(order_hash, "Extract OrderNo", "$.data.orderNo", "orderNo")
    response_assertion(order_hash, "Create Business Code 200",
                       "Assertion.response_data", '"code":200')

    save_jmx(root, output_path)


def save_jmx(root: ET.Element, output_path: str) -> None:
    """Pretty-print and save a JMeter XML plan."""
    rough_string = ET.tostring(root, encoding="utf-8")
    reparsed = minidom.parseString(rough_string)
    pretty = reparsed.toprettyxml(indent="  ")
    # Remove blank lines introduced by minidom
    lines = [line for line in pretty.splitlines() if line.strip()]
    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
        f.write("\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-dir", default=os.path.dirname(os.path.abspath(__file__)))
    parser.add_argument("--base-url", default="localhost")
    parser.add_argument("--port", default="8080")
    parser.add_argument("--product-id", default="13")
    parser.add_argument("--product-id-start", type=int, default=1)
    parser.add_argument("--product-id-end", type=int, default=15)
    parser.add_argument("--threads", type=int, default=100)
    parser.add_argument("--loops", type=int, default=30)
    parser.add_argument("--ramp-up", type=int, default=5)
    parser.add_argument("--csv", default="bench_users.csv")
    args = parser.parse_args()

    os.makedirs(args.output_dir, exist_ok=True)
    generate_product_detail_jmx(
        os.path.join(args.output_dir, "product_detail_cache.jmx"),
        args.base_url, args.port, args.product_id, args.product_id_start,
        args.product_id_end, args.threads, args.loops, args.ramp_up
    )
    generate_order_create_jmx(
        os.path.join(args.output_dir, "order_create.jmx"),
        args.csv, args.base_url, args.port, args.threads, args.loops, args.ramp_up
    )
    print(f"[generate] JMX files written to {args.output_dir}")


if __name__ == "__main__":
    main()
