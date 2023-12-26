import http from 'k6/http';
import { sleep } from 'k6';

export const options = {
  discardResponseBodies: true,
  scenarios: {
    contacts: {
      executor: 'constant-vus',
      vus: 25,
      duration: '10s',
    },
  },
};

// export const options = {
//   discardResponseBodies: true,
//   scenarios: {
//     contacts: {
//       executor: 'ramping-vus',
//       startVUs: 0,
//       stages: [
//         { duration: '20s', target: 10 },
//         { duration: '10s', target: 0 },
//       ],
//       gracefulRampDown: '0s',
//     },
//   },
// };
export default function () {
  http.post('http://localhost:8080/registration');
}



export function handleSummary(data) {
  return {
    'summary.json': JSON.stringify(data), //the default data object
  };
}

// export const options = {
//   discardResponseBodies: true,
//   scenarios: {
//     contacts: {
//       executor: 'ramping-vus',
//       startVUs: 0,
//       stages: [
//         { duration: '20s', target: 10 },
//         { duration: '10s', target: 0 },
//       ],
//       gracefulRampDown: '0s',
//     },
//   },
// };
