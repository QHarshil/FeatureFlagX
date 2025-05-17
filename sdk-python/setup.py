"""
This is a Python SDK for the FeatureFlagX service, which allows developers to manage feature flags
and configurations in their applications. The SDK provides a simple and efficient way to interact
with the FeatureFlagX API, enabling developers to easily toggle features on and off, manage user 
segments, and track feature usage.
"""

from setuptools import setup, find_packages

setup(
    name="featureflagx-sdk",
    version="0.1.0",
    description="Python SDK for FeatureFlagX Service",
    long_description=open("README.md").read(),
    long_description_content_type="text/markdown",
    packages=find_packages(include=["featureflagx", "featureflagx.*"]),
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
        "License :: OSI Approved :: MIT License",
        "Operating System :: OS Independent",
    ],
    python_requires=">=3.7",
)
