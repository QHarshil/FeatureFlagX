#!/usr/bin/env python

from setuptools import setup, find_packages

setup(
    name="featureflagx-sdk",
    version="0.1.0",
    author="Manus AI",
    author_email="contact@example.com", # Placeholder
    description="Python SDK for FeatureFlagX Service",
    long_description=open("README.md").read(),
    long_description_content_type="text/markdown",
    url="https://github.com/example/featureflagx", # Placeholder
    packages=find_packages(include=["featureflagx.sdk", "featureflagx.sdk.*"]),
    install_requires=[
        "requests>=2.20.0",
        "cachetools>=4.0.0",
    ],
    classifiers=[
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.7",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "License :: OSI Approved :: MIT License", # Assuming MIT License as per README plan
        "Operating System :: OS Independent",
    ],
    python_requires=">=3.7",
)

