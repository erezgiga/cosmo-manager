__author__ = 'dank'

import string
import json


def build_riemann_config(template, policies, rules, policies_events):
    
    policies_config = []

    for node_id, node_policies in policies.items():
        for node_policy_name, node_policy in node_policies.items():
            node_policy_events_template = policies_events[node_policy_name]['policy']

            aggregate_rules_message = " and ".join(filter(lambda message: message != '',
                                                          map(lambda rule: extract_message(rule['type'],
                                                                                           rule['properties'],
                                                                                           rules),
                                                              node_policy['rules'].values())))

            message = aggregate_rules_message
            #  add policy message to aggregated message.
            if 'message' in policies_events[node_policy_name]:
                message = policies_events[node_policy_name]['message'] + " : " + message

            node_policy = build_node_policy_config(node_id,
                                                   node_policy,
                                                   rules,
                                                   node_policy_events_template,
                                                   node_policy_name,
                                                   message)
            policies_config.append(node_policy)

    return string.Template(template).substitute(dict(
        events_mapping=''.join(policies_config)
    ))


def extract_message(rule_type, rule_properties, rules):
    if rule_type in rules:
        rule = rules[rule_type]
        if 'message' in rule:
            return string.Template(rule['message']).substitute(rule_properties)
    return ''


def build_node_policy_config(node_id,
                             node_policy,
                             rules,
                             node_policy_events_template,
                             node_policy_name,
                             message):

    event_json = build_node_policy_event(
        node_id,
        node_policy_name,
        node_policy['rules'], message)
    node_policy_events = string.Template(node_policy_events_template).substitute(dict(
        event=event_json,
        node_id=node_id
    ))

    node_policy_rules = []
    for rule in node_policy['rules'].values():
        rule_template = rules[rule['type']]['rule']
        rule_properties = rule['properties']
        rule_properties['node_id'] = node_id
        rule_properties['node_policy_events'] = node_policy_events
        rule_config = build_node_policy_rule_config(rule_template, rule_properties)
        node_policy_rules.append(rule_config)

    return node_policy_rules[0]


def build_node_policy_event(node_id, node_policy_name, node_policy_rules, message):
    event = {'app_id': node_id.split('.')[0], 'node_id': node_id, 'policy': node_policy_name,
             'message': message,
             'rule': node_policy_rules}
    return json.dumps(event).replace('"', '\\"')


def build_node_policy_rule_config(rule_template, properties):
    return string.Template(rule_template).substitute(properties)
