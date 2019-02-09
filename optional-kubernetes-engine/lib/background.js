// Copyright 2018, Google, Inc.
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

'use strict';

const {PubSub} = require('@google-cloud/pubsub');
const config = require('../config');
const logging = require('./logging');

const topicName = config.get('TOPIC_NAME');
const subscriptionName = config.get('SUBSCRIPTION_NAME');

const pubsub = new PubSub({
  projectId: config.get('GCLOUD_PROJECT'),
});

// This configuration will automatically create the topic if
// it doesn't yet exist. Usually, you'll want to make sure
// that a least one subscription exists on the topic before
// publishing anything to it as topics without subscribers
// will essentially drop any messages.
function getTopic(cb) {
  pubsub.createTopic(topicName, (err, topic) => {
    // topic already exists.
    logging.error("HEREERERER:getTopic");
    logging.error(JSON.stringify(err));
    if (err && err.code === 6) {
      logging.error("ERROR");
      cb(null, pubsub.topic(topicName));
      return;
    }
    cb(err, topic);
  });
}

// Used by the worker to listen to pubsub messages.
// When more than one worker is running they will all share the same
// subscription, which means that pub/sub will evenly distribute messages
// to each worker.
function subscribe(cb) {
  let subscription;

  // Event handlers
  function handleMessage(message) {
    const data = JSON.parse(message.data);
    message.ack();
    cb(null, data);
  }
  function handleError(err) {
    logging.error(err);
  }

  getTopic((err, topic) => {
    if (err) {
      logging.error("HEREERERER:subscribe");
      cb(err);
      return;
    }
    topic.

    topic.subscription(subscriptionName, (err, sub) => {
      logging.error("HEREERERER:subscription");
      logging.error(JSON.stringify(err));
      logging.error(JSON.stringify(sub));
      logging.error("HEREERERER:subscription-end");
      if (err) {
        cb(err);
        return;
      }

      subscription = sub;

      // Listen to and handle message and error events
      subscription.on('message', handleMessage);
      subscription.on('error', handleError);

      logging.info(
        `Listening to ${topicName} with subscription ${subscriptionName}`
      );
    });
  });

  // Subscription cancellation function
  return () => {
    if (subscription) {
      // Remove event listeners
      subscription.removeListener('message', handleMessage);
      subscription.removeListener('error', handleError);
      subscription = undefined;
    }
  };
}

// Adds a book to the queue to be processed by the worker.
function queueBook(bookId) {
  getTopic((err, topic) => {
    if (err) {
      logging.error('Error occurred while getting pubsub topic', err);
      return;
    }

    const data = {
      action: 'processBook',
      bookId: bookId,
    };

    topic.publish(Buffer.from(JSON.stringify(data)), err => {
      if (err) {
        logging.error('Error occurred while queuing background task', err);
      } else {
        logging.info(`Book ${bookId} queued for background processing`);
      }
    });
  });
}

module.exports = {
  subscribe,
  queueBook,
};
