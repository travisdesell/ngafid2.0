"""Startup check that the Kafka broker is configured and reachable.

Runs as the ``KAFKA`` category of the NGAFID startup validator: confirms the
Kafka client library is importable, resolves the bootstrap-servers address from
the effective configuration, and opens an admin-client connection to verify the
broker is reachable within the configured timeout.
"""

from __future__ import annotations

import importlib
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from validator import Validator


def run_check(validator: Validator) -> None:
    """Validate the Kafka driver, bootstrap address, and broker connectivity.

    Fails fast (recording a fail and returning) when the ``kafka`` admin client
    cannot be imported or when the bootstrap-servers address cannot be resolved.
    Otherwise attempts an admin-client connection using the validator's timeout
    and records whether the broker was reachable. All outcomes go through the
    validator's pass/fail helpers.

    Args:
        validator: The running startup validator; supplies the effective-property
            resolver, the CLI timeout, and the pass/fail recording helpers.
    """
    category = "KAFKA"
    try:
        kafka_admin_client = importlib.import_module("kafka").KafkaAdminClient
    except (ImportError, AttributeError) as exc:
        validator._fail(
            category,
            "kafka driver",
            str(exc),
            "rebuild ngafid-validate image to install validator dependencies",
        )
        return

    bootstrap = validator._effective_property("ngafid.kafka.bootstrap.servers")
    if not bootstrap:
        validator._fail(
            category,
            "bootstrap servers",
            "could not resolve kafka bootstrap servers",
            "set kafka bootstrap properties",
        )
        return

    try:
        admin = kafka_admin_client(
            bootstrap_servers=bootstrap,
            request_timeout_ms=validator.args.timeout * 1000,
        )
        validator._pass(category, "broker connection", f"connected to {bootstrap}")
    except Exception as exc:  # pylint: disable=broad-except
        validator._fail(
            category,
            "broker connection",
            str(exc),
            "verify kafka readiness and bootstrap address",
        )
        return

    try:
        topics = admin.list_topics()
        missing = [topic for topic in validator.required_topics if topic not in topics]
        if missing:
            validator._fail(
                category,
                "required topics",
                f"missing topics: {', '.join(missing)}",
                "ensure ngafid-kafka-topics completed successfully",
            )
        else:
            validator._pass(category, "required topics", "all required topics are present")

        _check_status_heartbeat_topic(validator, category, topics)

        _check_cluster_controller(validator, category, admin)
        _check_topic_topology(validator, category, admin)
    except Exception as exc:  # pylint: disable=broad-except
        validator._fail(
            category,
            "required topics",
            str(exc),
            "inspect kafka logs and topic creation step",
        )
    finally:
        try:
            admin.close()
        except Exception:  # pylint: disable=broad-except
            pass


def _check_status_heartbeat_topic(validator, category, topics):
    heartbeat_candidates = [topic for topic in topics if topic.endswith("status-heartbeat")]
    if heartbeat_candidates:
        validator._pass(
            category,
            "heartbeat topic",
            f"heartbeat topic present: {', '.join(sorted(heartbeat_candidates))}",
        )
    else:
        validator._fail(
            category,
            "heartbeat topic",
            "no status-heartbeat topic found",
            "ensure topic creation includes status heartbeat topic",
        )


def _check_cluster_controller(validator, category, admin):
    try:
        cluster_info = admin.describe_cluster()
        controller = cluster_info.get("controller")
        if controller is None:
            validator._pass(
                category,
                "cluster controller",
                "controller not reported by client metadata; skipping strict controller validation",
            )
        else:
            validator._pass(category, "cluster controller", f"controller={controller}")
    except Exception as exc:  # pylint: disable=broad-except
        validator._fail(
            category,
            "cluster controller",
            str(exc),
            "inspect Kafka broker/controller logs",
        )


def _check_topic_topology(validator, category, admin):
    try:
        descriptions = admin.describe_topics(validator.required_topics)
    except Exception as exc:  # pylint: disable=broad-except
        validator._fail(
            category,
            "topic topology",
            str(exc),
            "verify topic metadata access and broker readiness",
        )
        return

    for topic_description in descriptions:
        topic_name = topic_description.get("topic") or "<unknown-topic>"
        partitions = topic_description.get("partitions") or []
        partition_count = len(partitions)

        if partition_count != validator.expected_kafka_partitions:
            validator._fail(
                category,
                f"topic partitions {topic_name}",
                f"expected {validator.expected_kafka_partitions}, found {partition_count}",
                "recreate topics using org.ngafid.core.kafka.Topic",
            )
        else:
            validator._pass(
                category,
                f"topic partitions {topic_name}",
                f"partition count={partition_count}",
            )

        replication_factors = set()
        for partition in partitions:
            replicas = partition.get("replicas") or []
            replication_factors.add(len(replicas))

        if replication_factors != {validator.expected_kafka_replication_factor}:
            validator._fail(
                category,
                f"topic replication {topic_name}",
                f"expected replication factor {validator.expected_kafka_replication_factor}, found {sorted(replication_factors)}",  # noqa: E501
                "recreate topics with expected replication factor",
            )
        else:
            validator._pass(
                category,
                f"topic replication {topic_name}",
                f"replication factor={validator.expected_kafka_replication_factor}",
            )
